package com.cs5520group15.memorycircle.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
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
        var incomingPrimed = false
        incomingListener = db.collection("users").document(uid)
            .collection("incomingRequests")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                if (!incomingPrimed) { incomingPrimed = true; return@addSnapshotListener }
                if (!settings().newFriendRequests) return@addSnapshotListener

                snap.documentChanges.forEach { change ->
                    if (change.type != DocumentChange.Type.ADDED) return@forEach
                    val status = change.document.getString("status").orEmpty().uppercase()
                    // Treat missing status as PENDING for legacy docs.
                    if (status.isNotEmpty() && status != "PENDING") return@forEach
                    val name = change.document.getString("name").orEmpty()
                        .ifBlank { "Someone" }
                    NotificationService.show(
                        ctx,
                        title = "New friend request",
                        body  = "$name wants to be your friend"
                    )
                }
            }

        // 2) Groups I'm a member of. ADDED events (where I'm not the owner)
        //    surface as "you've been invited". Also drives per-group post
        //    listener lifecycle: attach when a group appears, detach when
        //    it goes away.
        var groupsPrimed = false
        groupsListener = db.collection("groups")
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                snap.documentChanges.forEach { change ->
                    val gid       = change.document.id
                    val ownerId   = change.document.getString("ownerId")
                    val groupName = change.document.getString("name") ?: "Untitled"
                    groupNames[gid] = groupName

                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            attachPostListener(gid)
                            if (groupsPrimed && ownerId != uid && settings().newGroupActivity) {
                                NotificationService.show(
                                    ctx,
                                    title = "Added to a group",
                                    body  = "You've been added to \"$groupName\""
                                )
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            // No notification — owned-group joiner case is
                            // handled by ownedGroupsListener.
                        }
                        DocumentChange.Type.REMOVED -> {
                            detachPostListener(gid)
                            groupNames.remove(gid)
                        }
                    }
                }

                if (!groupsPrimed) groupsPrimed = true
            }

        // 3) Groups I own — watch memberIds for new joiners. Diff against
        //    the previous member set so we can name each new joiner.
        var ownedPrimed = false
        ownedGroupsListener = db.collection("groups")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                snap.documents.forEach { doc ->
                    val gid       = doc.id
                    val groupName = doc.getString("name") ?: "Untitled"
                    @Suppress("UNCHECKED_CAST")
                    val current   = ((doc.get("memberIds") as? List<String>) ?: emptyList()).toSet()
                    val previous  = ownedGroupMembers[gid]
                    ownedGroupMembers[gid] = current

                    if (ownedPrimed && previous != null && settings().newGroupActivity) {
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
                }

                if (!ownedPrimed) ownedPrimed = true
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
        var postsPrimed = false

        perGroupPostListeners[gid] = db.collection("groups").document(gid)
            .collection("scrapbooks").document(currentMonth)
            .collection("posts")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                val me        = AuthRepository.currentUid
                val groupName = groupNames[gid] ?: "your group"

                snap.documentChanges.forEach { change ->
                    val doc          = change.document
                    val postId       = doc.id
                    val authorId     = doc.getString("authorId")
                    @Suppress("UNCHECKED_CAST")
                    val photos       = (doc.get("photos") as? List<Map<String, Any?>>) ?: emptyList()
                    val commentCount = doc.getLong("commentCount") ?: 0L
                    val newState     = PostState(photos.size, commentCount)
                    val prevState    = postState[postId]

                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            if (postsPrimed && authorId != null && authorId != me
                                && settings().newMemoryPosts) {
                                scope.launch {
                                    val authorName = AuthRepository.getUserName(authorId) ?: "Someone"
                                    NotificationService.show(
                                        ctx,
                                        title = "New post in $groupName",
                                        body  = "$authorName posted a new memory"
                                    )
                                }
                            }
                            postState[postId] = newState
                        }

                        DocumentChange.Type.MODIFIED -> {
                            if (postsPrimed && prevState != null && settings().newMemoryPosts) {
                                // New photos: array grew → the suffix beyond
                                // prevState.photoCount is the freshly-added
                                // batch. Fire one notification per uploader
                                // != me.
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

                                // New comments: commentCount went up. The post
                                // doc doesn't carry the commenter's uid, so a
                                // one-shot read on the latest comment fetches
                                // it. Could re-fire on echoed snapshots if the
                                // count climbs by N then snaps back — fine for
                                // a demo.
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
                            postState[postId] = newState
                        }

                        DocumentChange.Type.REMOVED -> {
                            postState.remove(postId)
                        }
                    }
                }

                if (!postsPrimed) postsPrimed = true
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
    }

    private fun settings() = NotificationSettingsRepository.settings.value
}
