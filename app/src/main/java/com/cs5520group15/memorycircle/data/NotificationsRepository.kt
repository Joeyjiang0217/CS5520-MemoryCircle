/**
 * What: Process-lifetime singleton that watches Firestore (friend requests,
 *       group membership, posts / photos / comments) and turns new events into
 *       Android system notifications via NotificationService, gated by
 *       NotificationSettingsRepository.
 * Who: Started by MainActivity.onCreate; also driven by SeedRepository's
 *       simulate* dev-tools writes (via DevToolsViewModel) to fire test events.
 * When: init(ctx) is called once at app start; its AuthStateListener then
 *       attaches listeners on sign-in and detaches them on sign-out for the
 *       whole process lifetime.
 */

package com.cs5520group15.memorycircle.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.YearMonth

/**
 * What: Translates Firestore change events into Android system notifications.
 *       Holds its own set of snapshot listeners (separate from the ones that
 *       drive the UI — these survive screen-level binding) and gates every
 *       fire on the user-toggleable channels in NotificationSettingsRepository.
 *
 *       Six event types, mapped to three notification channels:
 *         newFriendRequests channel
 *           - incoming friend request received (new doc on
 *             users/{me}/incomingRequests with status=PENDING)
 *         newGroupActivity channel
 *           - I was added to someone else's group (new group doc has me in
 *             memberIds but ownerId != me)
 *           - new member joined a group I own (groups whereEqualTo ownerId=me,
 *             memberIds gained a uid that isn't me)
 *         newMemoryPosts channel
 *           - new post in any group I'm in (post.authorId != me)
 *           - new photo appended to a post (photos array grew, last
 *             uploader != me)
 *           - new comment on a post (commentCount went up; latest comment's
 *             authorId resolved via a one-shot read)
 *
 *       Lifecycle is bound to FirebaseAuth state — listeners attach when a
 *       user signs in and detach on sign-out. The repo skips the FIRST
 *       snapshot from each listener so the "initial load" of existing data
 *       doesn't fire a flurry of notifications on app start; only diffs
 *       after that point trigger.
 *
 *       Per-group listeners (for posts/photos/comments) are dynamically
 *       attached as groups appear in the user's group list and removed
 *       when they leave. Each group's listener is scoped to the CURRENT
 *       month's scrapbook only — older months can't get new activity, and
 *       this keeps the listener count proportional to active groups.
 *
 * Who: Singleton attached once from MainActivity.onCreate via init(ctx).
 * When: Runs for the entire process lifetime; the AuthStateListener handles
 *       sign-in / sign-out re-binding automatically.
 */
object NotificationsRepository {

    private val db    = FirebaseModule.db
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var appContext: Context? = null
    private var boundUid:   String?  = null

    private var incomingListener:    ListenerRegistration? = null
    private var groupsListener:      ListenerRegistration? = null
    private var ownedGroupsListener: ListenerRegistration? = null
    private val perGroupPostListeners = mutableMapOf<String, ListenerRegistration>()

    /** Last-known memberIds set per owned group, so we can diff new joiners. */
    private val ownedGroupMembers = mutableMapOf<String, Set<String>>()

    /** Last-known post snapshot per group's current-month scrapbook, keyed
     *  by postId. Used to detect photo / comment-count deltas. */
    private val perGroupPostState = mutableMapOf<String, MutableMap<String, PostState>>()

    /** Group name cache so notification bodies don't have to round-trip. */
    private val groupNames = mutableMapOf<String, String>()

    /** Sender uids we've already fired a "new friend request" notification
     *  for. Cleared per-doc when the doc disappears (user swiped it away,
     *  server rejected the optimistic write, etc.). Lets us dedupe over the
     *  multiple snapshot fires Firestore's INCLUDE-metadata stream emits
     *  for a single optimistic-write → server-confirm cycle. */
    private val notifiedRequestDocIds = mutableSetOf<String>()

    /** Group ids we've already fired a "you've been added to a group"
     *  notification for. Same dedupe role as notifiedRequestDocIds — sized
     *  per the user's group membership. */
    private val notifiedJoinedGroupIds = mutableSetOf<String>()

    private data class PostState(val photoCount: Int, val commentCount: Long)

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        if (uid == boundUid) return@AuthStateListener
        detachListeners()
        boundUid = uid
        if (uid != null) attachListeners(uid)
    }

    /**
     * Wires the channel + auth listener. Idempotent — safe to call from
     * MainActivity.onCreate even if a previous Activity instance already
     * called it.
     */
    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        NotificationService.init(appContext!!)
        FirebaseModule.auth.addAuthStateListener(authStateListener)
    }

    // ── Listener attach ─────────────────────────────────────────────────────

    private fun attachListeners(uid: String) {
        val ctx = appContext ?: return

        // 1) Friend requests inbox.
        //
        // Why this iterates `snap.documents` with a dedupe set instead of the
        // simpler `documentChanges + ADDED` shape:
        //
        // - Firestore applies local writes to the cache optimistically before
        //   the server round-trips. With the default MetadataChanges.EXCLUDE
        //   listener, a write of a doc to my own inbox (e.g. a DevTool sim
        //   run from a dev account) fires the listener ONCE — at the moment
        //   the local cache flips — with `hasPendingWrites=true`. The server-
        //   side acknowledgment is a metadata-only update and does not
        //   produce a second fire under EXCLUDE.
        // - If we then skip on `hasPendingWrites=true` to filter out writes
        //   the server will eventually reject (the non-dev sim case), we
        //   also skip the only fire we ever get for writes the server
        //   eventually accepts (the dev sim case). Result: dev account
        //   testing sim sees zero notification.
        // - Switching to MetadataChanges.INCLUDE makes Firestore emit a
        //   second fire when the metadata transitions (pending → confirmed)
        //   — but the per-document change.type for that fire is MODIFIED,
        //   indistinguishable from a legitimate data change.
        //
        // So we sidestep the ADDED/MODIFIED dance entirely: on every fire,
        // walk `snap.documents`, treat any server-confirmed PENDING doc
        // whose id we haven't notified for as "new arrival", fire once,
        // then remember the id. retainAll at the end drops ids of docs the
        // user manually deleted (so a future re-sent request from the same
        // sender notifies again) and ids the server rolled back.
        var incomingPrimed = false
        incomingListener = db.collection("users").document(uid)
            .collection("incomingRequests")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                if (!incomingPrimed) {
                    // Seed dedupe set with currently-existing docs so app
                    // start doesn't fire notifications for pre-existing
                    // pending requests.
                    snap.documents.forEach { notifiedRequestDocIds.add(it.id) }
                    incomingPrimed = true
                    return@addSnapshotListener
                }

                if (settings().newFriendRequests) {
                    snap.documents.forEach { doc ->
                        if (doc.metadata.hasPendingWrites()) return@forEach
                        if (doc.id in notifiedRequestDocIds) return@forEach
                        val status = doc.getString("status").orEmpty().uppercase()
                        // Treat missing status as PENDING for legacy docs.
                        // Always remember the id even when we skip firing,
                        // so a doc that lands as ACCEPTED/DECLINED (which
                        // shouldn't normally happen but is defensively
                        // possible if Firestore Console writes it that way)
                        // doesn't fire later if its status flips.
                        notifiedRequestDocIds.add(doc.id)
                        if (status.isNotEmpty() && status != "PENDING") return@forEach
                        val name = doc.getString("name").orEmpty()
                            .ifBlank { "Someone" }
                        NotificationService.show(
                            ctx,
                            title = "New friend request",
                            body  = "$name wants to be your friend"
                        )
                    }
                }

                // Clean up dedupe set: ids no longer in the snapshot have
                // been deleted (manual swipe, or server rolled back an
                // optimistic write). Drop them so a re-arrival of the same
                // sender notifies again.
                val currentIds = snap.documents.map { it.id }.toSet()
                notifiedRequestDocIds.retainAll(currentIds)
            }

        // 2) Groups I'm a member of. Same INCLUDE-metadata + dedupe-set
        //    treatment as the incoming-requests listener so dev sims that
        //    write a new group from the dev's own device get a server-
        //    confirm fire and successfully notify. Also drives per-group
        //    post listener lifecycle: attach when a group appears, detach
        //    when it goes away.
        var groupsPrimed = false
        groupsListener = db.collection("groups")
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                val currentGroupIds = snap.documents.map { it.id }.toSet()

                // Maintain the post-listener lifecycle regardless of pending
                // status — fresh attach is cheap and a brief over-attachment
                // on an eventually-rejected write doesn't fire anything
                // (the post listener has its own hasPendingWrites guard).
                snap.documents.forEach { doc ->
                    val gid = doc.id
                    groupNames[gid] = doc.getString("name") ?: "Untitled"
                    if (gid !in perGroupPostListeners.keys) {
                        attachPostListener(gid)
                    }
                }
                val toDetach = perGroupPostListeners.keys - currentGroupIds
                toDetach.forEach { gid ->
                    detachPostListener(gid)
                    groupNames.remove(gid)
                }

                // Priming: seed the dedupe set with every group already in
                // my membership so app start doesn't notify for pre-existing
                // group invites.
                if (!groupsPrimed) {
                    notifiedJoinedGroupIds.addAll(currentGroupIds)
                    groupsPrimed = true
                } else if (settings().newGroupActivity) {
                    snap.documents.forEach { doc ->
                        if (doc.metadata.hasPendingWrites()) return@forEach
                        val gid = doc.id
                        if (gid in notifiedJoinedGroupIds) return@forEach
                        notifiedJoinedGroupIds.add(gid)
                        val ownerId = doc.getString("ownerId")
                        if (ownerId == uid) return@forEach
                        val groupName = doc.getString("name") ?: "Untitled"
                        NotificationService.show(
                            ctx,
                            title = "Added to a group",
                            body  = "You've been added to \"$groupName\""
                        )
                    }
                }

                // Drop dedupe entries for groups I no longer belong to
                // (left / kicked / deleted). Re-joining the same group
                // would notify again.
                notifiedJoinedGroupIds.retainAll(currentGroupIds)
            }

        // 3) Groups I own — watch memberIds for new joiners. INCLUDE-
        //    metadata + skip-pending so my own optimistic writes (e.g.
        //    dev sim "Test User 10 joins your owned group" adding a
        //    memberId) wait for server confirmation before being diffed.
        //    Without skipping pending we'd update the baseline from the
        //    optimistic snapshot, then the server-confirm fire would see
        //    `current == previous` and no joiner notification would fire.
        //    Without INCLUDE the server confirmation wouldn't get a
        //    separate fire at all (it's a metadata-only update).
        //
        //    Priming logic is per-doc rather than global: first time we
        //    see a doc server-confirmed we just baseline it without firing
        //    (previous == null). Subsequent server-confirmed fires diff
        //    against that baseline. This correctly handles the edge case
        //    where the very first listener fire after attach contains an
        //    optimistic-pending write (we skip it, don't baseline, and
        //    fire on the server confirmation that follows).
        ownedGroupsListener = db.collection("groups")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                snap.documents.forEach { doc ->
                    if (doc.metadata.hasPendingWrites()) return@forEach
                    val gid       = doc.id
                    val groupName = doc.getString("name") ?: "Untitled"
                    @Suppress("UNCHECKED_CAST")
                    val current   = ((doc.get("memberIds") as? List<String>) ?: emptyList()).toSet()
                    val previous  = ownedGroupMembers[gid]

                    if (previous != null && settings().newGroupActivity) {
                        // New joiners = current minus previous minus me (the owner).
                        val joiners = current - previous - setOf(uid)
                        joiners.forEach { joinerUid ->
                            scope.launch {
                                val joinerName = AuthRepository.getUserName(joinerUid) ?: "Someone"
                                NotificationService.show(
                                    ctx,
                                    title = "New group member",
                                    body  = "$joinerName joined \"$groupName\""
                                )
                            }
                        }
                    }
                    // Baseline AFTER diff so the previous==null branch
                    // (first-time-seen) doesn't fire.
                    ownedGroupMembers[gid] = current
                }

                // Drop baselines for groups I no longer own.
                val currentOwnedIds = snap.documents.map { it.id }.toSet()
                ownedGroupMembers.keys.retainAll(currentOwnedIds)
            }
    }

    // ── Per-group post listener ────────────────────────────────────────────

    /**
     * Attaches a snapshot listener on `groups/{gid}/scrapbooks/{currentMonth}/posts`
     * so we can fire notifications for new posts, new photos on existing posts,
     * and new comments on existing posts — all without holding N×M listeners
     * (one per post per group).
     *
     * Idempotent — calling twice for the same group is a no-op.
     */
    private fun attachPostListener(gid: String) {
        if (perGroupPostListeners.containsKey(gid)) return
        val ctx = appContext ?: return
        val currentMonth = YearMonth.now().toString()
        val postState = mutableMapOf<String, PostState>()
        perGroupPostState[gid] = postState
        // Global priming flag for THIS post listener. First fire baselines
        // every server-confirmed post without notifying, so attaching the
        // listener to a group that already has historical posts doesn't
        // spray "new post" notifications for stale data. After priming,
        // a post seen for the first time (prevState == null) is a real
        // new arrival and notifies normally.
        var postsPrimed = false

        // Same INCLUDE-metadata + skip-pending + per-doc baselining we
        // use on the other listeners — lets a dev's own optimistic post /
        // photo / comment write notify on the server-confirm fire instead
        // of being eaten by the optimistic ADDED that we skip.
        perGroupPostListeners[gid] = db.collection("groups").document(gid)
            .collection("scrapbooks").document(currentMonth)
            .collection("posts")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                val me        = AuthRepository.currentUid
                val groupName = groupNames[gid] ?: "your group"

                if (!postsPrimed) {
                    snap.documents.forEach { doc ->
                        if (doc.metadata.hasPendingWrites()) return@forEach
                        @Suppress("UNCHECKED_CAST")
                        val photos       = (doc.get("photos") as? List<Map<String, Any?>>) ?: emptyList()
                        val commentCount = doc.getLong("commentCount") ?: 0L
                        postState[doc.id] = PostState(photos.size, commentCount)
                    }
                    postsPrimed = true
                    return@addSnapshotListener
                }

                snap.documents.forEach { doc ->
                    if (doc.metadata.hasPendingWrites()) return@forEach

                    val postId       = doc.id
                    val authorId     = doc.getString("authorId")
                    @Suppress("UNCHECKED_CAST")
                    val photos       = (doc.get("photos") as? List<Map<String, Any?>>) ?: emptyList()
                    val commentCount = doc.getLong("commentCount") ?: 0L
                    val newState     = PostState(photos.size, commentCount)
                    val prevState    = postState[postId]

                    if (prevState == null) {
                        // First server-confirmed view of this post. Fire a
                        // "new post" notification if it's not authored by
                        // me; otherwise silently baseline.
                        if (authorId != null && authorId != me && settings().newMemoryPosts) {
                            scope.launch {
                                val authorName = AuthRepository.getUserName(authorId) ?: "Someone"
                                NotificationService.show(
                                    ctx,
                                    title = "New post in $groupName",
                                    body  = "$authorName posted a new memory"
                                )
                            }
                        }
                    } else if (settings().newMemoryPosts) {
                        // Post existed before; diff for new photos / comments.
                        if (photos.size > prevState.photoCount) {
                            val freshlyAdded = photos.drop(prevState.photoCount)
                            freshlyAdded.forEach { photo ->
                                val uploaderId = photo["uploaderId"] as? String
                                if (uploaderId != null && uploaderId != me) {
                                    scope.launch {
                                        val uploaderName = AuthRepository.getUserName(uploaderId) ?: "Someone"
                                        NotificationService.show(
                                            ctx,
                                            title = "New photo in $groupName",
                                            body  = "$uploaderName added a photo"
                                        )
                                    }
                                }
                            }
                        }
                        if (commentCount > prevState.commentCount) {
                            scope.launch {
                                val newest = runCatching {
                                    doc.reference.collection("comments")
                                        .orderBy("createdAt", Query.Direction.DESCENDING)
                                        .limit(1)
                                        .get().await()
                                        .documents.firstOrNull()
                                }.getOrNull() ?: return@launch
                                val commenterUid = newest.getString("authorId") ?: return@launch
                                if (commenterUid == me) return@launch
                                val commenterName = AuthRepository.getUserName(commenterUid) ?: "Someone"
                                val text = newest.getString("text").orEmpty().ifBlank { "commented" }
                                NotificationService.show(
                                    ctx,
                                    title = "New comment in $groupName",
                                    body  = "$commenterName: $text"
                                )
                            }
                        }
                    }

                    // Baseline AFTER firing so the prevState==null branch
                    // doesn't get confused.
                    postState[postId] = newState
                }

                // Drop baselines for posts that disappeared (server
                // rollback, manual deletion, month-rollover).
                val currentPostIds = snap.documents.map { it.id }.toSet()
                postState.keys.retainAll(currentPostIds)
            }
    }

    private fun detachPostListener(gid: String) {
        perGroupPostListeners.remove(gid)?.remove()
        perGroupPostState.remove(gid)
    }

    private fun detachListeners() {
        incomingListener?.remove();    incomingListener    = null
        groupsListener?.remove();      groupsListener      = null
        ownedGroupsListener?.remove(); ownedGroupsListener = null
        perGroupPostListeners.values.forEach { it.remove() }
        perGroupPostListeners.clear()
        perGroupPostState.clear()
        ownedGroupMembers.clear()
        groupNames.clear()
        notifiedRequestDocIds.clear()
        notifiedJoinedGroupIds.clear()
    }

    private fun settings() = NotificationSettingsRepository.settings.value
}
