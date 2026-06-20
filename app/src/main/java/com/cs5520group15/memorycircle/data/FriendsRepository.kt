/**
 * What: Live source of truth for the friend list, the friend-request queue, the
 *       group list on the Friends tab, plus the add-friend search and the
 *       send / accept / decline / delete friend-request actions, all over Firestore.
 * Who: Used by FriendsViewModel, FriendsSearchViewModel, AddFriendSearchViewModel,
 *       AllFriendRequestsViewModel, and CreateGroupViewModel.
 * When: bind() is called from those ViewModels' init blocks to attach the
 *       listeners; they detach on logout or when no user is signed in.
 */

package com.cs5520group15.memorycircle.data

import com.cs5520group15.memorycircle.model.Friend
import com.cs5520group15.memorycircle.model.FriendRequest
import com.cs5520group15.memorycircle.model.GroupSummary
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * What: Source of truth for the friend list, the friend-request queue, the
 *       group list shown on the Friends tab, and the broader "discoverable"
 *       pool used by the add-friend search.
 *
 *       Friends and groups are live Firestore subscriptions:
 *         - friends: users/{uid}/friends subcollection. Each doc id is the
 *           friend's uid; the body just timestamps when the friendship was
 *           created.
 *         - groups:  same array-contains query the Home screen uses.
 *
 *       Display names + avatars are kept fresh cross-device by attaching a
 *       per-uid snapshot listener on users/{uid} for every uid appearing in
 *       the friend list or any group's memberIds. When a friend (or a member
 *       of one of my groups) edits their own profile elsewhere, that change
 *       fires the corresponding listener and the friend / group flow
 *       republishes — no manual cache invalidation, no waiting on a refresh.
 *
 *       Friend requests are also live: incomingRequests is subscribed
 *       (orderBy requestedAt desc). Pending docs are unbounded; the
 *       actioned-history (ACCEPTED + DECLINED) is capped at 10 in the DB
 *       itself by pruneActionedHistory, which runs after every accept /
 *       decline — so storage doesn't bloat indefinitely and the UI shows
 *       exactly what's in the DB. accept/decline flip the `status` field
 *       on the incoming doc (kept around so the See-all page still shows
 *       the history row) while removing the sender's outgoingRequest;
 *       only a manual deleteRequest actually drops the row.
 *
 *       The "discoverable" pool and the locally-sent invite set are still
 *       mock — kept around so older surfaces compile, but nothing in the
 *       UI consumes them today.
 *
 * Who: Used by FriendsViewModel, FriendsSearchViewModel, and
 *      AllFriendRequestsViewModel.
 * When: bind() is called from FriendsViewModel.init / the See-all VM's
 *       init; listeners detach on logout or when no user is signed in.
 */
object FriendsRepository {

    private val db = FirebaseModule.db

    private val _friends           = MutableStateFlow<List<Friend>>(emptyList())
    private val _requests          = MutableStateFlow<List<FriendRequest>>(emptyList())
    private val _groups            = MutableStateFlow<List<GroupSummary>>(emptyList())
    private val _discoverableUsers = MutableStateFlow<List<Friend>>(emptyList())
    private val _invitedUserIds    = MutableStateFlow<Set<String>>(emptySet())
    /** uids the signed-in user has an outstanding outgoing friend request to
     *  (request sent, not yet accepted). Drives the "Invitation sent" pill in
     *  AddFriendSearch. Cleared automatically when the request is accepted /
     *  declined / cancelled, via the outgoingRequests subcollection listener. */
    private val _outgoingRequests  = MutableStateFlow<Set<String>>(emptySet())

    val friends:           StateFlow<List<Friend>>        = _friends.asStateFlow()
    val requests:          StateFlow<List<FriendRequest>> = _requests.asStateFlow()
    val groups:            StateFlow<List<GroupSummary>>  = _groups.asStateFlow()
    val discoverableUsers: StateFlow<List<Friend>>        = _discoverableUsers.asStateFlow()
    val invitedUserIds:    StateFlow<Set<String>>         = _invitedUserIds.asStateFlow()
    val outgoingRequests:  StateFlow<Set<String>>         = _outgoingRequests.asStateFlow()

    private var friendsListener:          ListenerRegistration? = null
    private var groupsListener:           ListenerRegistration? = null
    private var outgoingRequestsListener: ListenerRegistration? = null
    private var incomingRequestsListener: ListenerRegistration? = null
    private var boundUid:                 String? = null

    /**
     * Raw inputs from the friends / groups listeners — held so we can rebuild
     * the published flows whenever any of the per-uid user listeners fire.
     */
    private var lastFriendUids: List<String> = emptyList()
    private var lastGroups: List<Triple<String, String, List<String>>> = emptyList()

    /**
     * Per-uid live brief state. Each entry pairs the snapshot listener on
     * users/{uid} with the latest UserCard read from it. The set is the union
     * of friend uids + every group's memberIds, deduped, so two groups sharing
     * a member only cost one listener.
     */
    private data class UserCard(
        val name:        String,
        val avatarUrl:   String,
        val emailMasked: String,
        val bio:         String
    )
    private val userListeners = mutableMapOf<String, ListenerRegistration>()
    private val userCards     = mutableMapOf<String, UserCard>()

    init { seedMockRequestsAndDiscoverable() }

    /**
     * Attaches Firestore listeners for the currently signed-in user. Idempotent
     * for the same uid; rebinds cleanly when the user changes (e.g. logout +
     * re-login). Detaches everything and clears the lists when no user is
     * signed in.
     */
    fun bind() {
        val uid = AuthRepository.currentUid
        if (uid == boundUid && (friendsListener != null || groupsListener != null)) return
        detach()
        boundUid = uid
        if (uid == null) {
            _friends.value = emptyList()
            _groups.value  = emptyList()
            return
        }

        // 1) Friends subcollection — doc ids are the friend's uid.
        friendsListener = db.collection("users").document(uid)
            .collection("friends")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                lastFriendUids = snap.documents.map { it.id }
                syncUserListeners()
                rebuildFriends()
            }

        // 2) Groups query — same shape as HomeViewModel; we also pre-resolve
        //    up to 9 member avatar URLs per group so the FriendsScreen Groups
        //    tab can render its collage without an extra round trip.
        groupsListener = db.collection("groups")
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                @Suppress("UNCHECKED_CAST")
                lastGroups = snap.documents.map { doc ->
                    val ids = (doc.get("memberIds") as? List<String>).orEmpty()
                    Triple(doc.id, doc.getString("name") ?: "Untitled", ids)
                }
                syncUserListeners()
                rebuildGroups()
            }

        // 3) Outgoing friend requests — drives the "Invitation sent" pill in
        //    AddFriendSearch. Each doc id is the target uid.
        outgoingRequestsListener = db.collection("users").document(uid)
            .collection("outgoingRequests")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _outgoingRequests.value = snap.documents.map { it.id }.toSet()
            }

        // 4) Incoming friend requests — drives the FRIEND REQUESTS preview on
        //    FriendsScreen and the full list on AllFriendRequestsScreen.
        //    Each doc id is the sender uid; accepted/declined docs remain in
        //    the subcollection with a status marker (only manual delete
        //    actually removes them).
        //
        //    The actioned-history cap is enforced at WRITE time —
        //    pruneActionedHistory(uid) runs after every accept / decline so
        //    the subcollection never holds more than 10 actioned docs. The
        //    listener mirrors whatever is in the DB; pending is unbounded,
        //    so the orderBy + limit(200) is just a safety ceiling for the
        //    pathological pending case.
        incomingRequestsListener = db.collection("users").document(uid)
            .collection("incomingRequests")
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val all = snap.documents.map { doc ->
                    val statusStr = doc.getString("status").orEmpty().uppercase()
                    val status = runCatching { FriendRequest.Status.valueOf(statusStr) }
                        .getOrDefault(FriendRequest.Status.PENDING)
                    FriendRequest(
                        id             = doc.id,
                        fromUserId     = doc.id,
                        fromUserName   = doc.getString("name").orEmpty(),
                        fromUserEmail  = "",      // public users doc no longer carries email
                        mutualFriends  = 0,       // not computed (would require a friends ∩ friends scan)
                        status         = status,
                        fromUserBio    = doc.getString("bio").orEmpty()
                    )
                }
                // Pending first (newest by requestedAt), then actioned (also
                // newest first via the orderBy). DB cap means actioned size
                // is always ≤ 10; no client-side slice needed.
                val pending  = all.filter { it.status == FriendRequest.Status.PENDING }
                val actioned = all.filter { it.status != FriendRequest.Status.PENDING }
                _requests.value = pending + actioned
            }
    }

    /**
     * Diffs the desired uid set (friend uids ∪ every group's memberIds) against
     * the listeners we currently hold, attaches a snapshot listener on
     * users/{uid} for every newly-needed id, and removes the ones no longer
     * referenced. Each listener writes its latest card into [userCards] and
     * republishes both flows so consumers see the new name / avatar.
     */
    private fun syncUserListeners() {
        val needed = (lastFriendUids + lastGroups.flatMap { it.third }).toSet()

        // Detach listeners for uids no longer referenced.
        val toDetach = userListeners.keys - needed
        toDetach.forEach { uid ->
            userListeners.remove(uid)?.remove()
            userCards.remove(uid)
        }

        // Attach listeners for newly-needed uids.
        val toAttach = needed - userListeners.keys
        toAttach.forEach { uid ->
            userListeners[uid] = db.collection("users").document(uid)
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) return@addSnapshotListener
                    userCards[uid] = UserCard(
                        name        = snap.getString("name").orEmpty(),
                        avatarUrl   = snap.getString("avatarUrl").orEmpty(),
                        emailMasked = snap.getString("emailMasked").orEmpty(),
                        bio         = snap.getString("bio").orEmpty()
                    )
                    // Either path may depend on this uid — republish both.
                    rebuildFriends()
                    rebuildGroups()
                }
        }
    }

    private fun rebuildFriends() {
        _friends.value = lastFriendUids
            .map { uid ->
                val card = userCards[uid]
                Friend(
                    id             = uid,
                    name           = card?.name.orEmpty(),
                    email          = card?.emailMasked.orEmpty(),
                    sharedMemories = 0,
                    isOnline       = false,
                    avatarUrl      = card?.avatarUrl.orEmpty(),
                    bio            = card?.bio.orEmpty()
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun rebuildGroups() {
        _groups.value = lastGroups.map { (gid, name, memberIds) ->
            val collage = memberIds.take(9)
            GroupSummary(
                id               = gid,
                name             = name,
                memberCount      = memberIds.size,
                memberAvatarUrls = collage.map { userCards[it]?.avatarUrl.orEmpty() },
                memberNames      = collage.map { userCards[it]?.name.orEmpty() }
            )
        }.sortedBy { it.name.lowercase() }
    }

    fun detach() {
        friendsListener?.remove();          friendsListener          = null
        groupsListener?.remove();           groupsListener           = null
        outgoingRequestsListener?.remove(); outgoingRequestsListener = null
        incomingRequestsListener?.remove(); incomingRequestsListener = null
        userListeners.values.forEach { it.remove() }
        userListeners.clear()
        userCards.clear()
        lastFriendUids          = emptyList()
        lastGroups              = emptyList()
        _outgoingRequests.value = emptySet()
        _requests.value         = emptyList()
        boundUid = null
    }

    /**
     * What: One-shot Firestore search for the "add new friend" flow.
     *
     *       Full-email queries match against `emailHash` (SHA-256 of the
     *       normalized address) — emailMasked is too lossy to be an index
     *       (multiple addresses can collapse to the same mask). Plain-text
     *       queries do a Firestore prefix search against `nameLower`
     *       (orderBy + startAt/endAt) capped at 20, so "test", "Test",
     *       and "TEST" all match "Test User 1". Self and already-friend
     *       uids are filtered out so the picker only surfaces actionable
     *       rows.
     *
     *       Both branches ride Firestore's B-tree index, so the cost
     *       stays O(log N + K) on the matched-row count — no linear
     *       scan over the users collection even at very large N.
     *
     * Who: Used by AddFriendSearchViewModel.submit().
     * When: Per Search-key press in the add-friend search overlay.
     */
    suspend fun searchUsers(query: String): List<Friend> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()

        val me = AuthRepository.currentUid
        val myFriendIds = _friends.value.map { it.id }.toSet()

        return try {
            val snap = if (EmailShape.matches(needle)) {
                // Full email → hash the same way registration does and
                // exact-match against the public emailHash field so each
                // address resolves to a unique user (no mask collisions).
                db.collection("users")
                    .whereEqualTo("emailHash", AuthRepository.emailHash(needle))
                    .limit(20)
                    .get().await()
            } else {
                // Name prefix on the lowercased index field, so the
                // query is case-insensitive. The
                // `` upper bound is the standard high-codepoint
                // sentinel for Firestore prefix range queries.
                val lower = AuthRepository.nameLower(needle)
                db.collection("users")
                    .orderBy("nameLower")
                    .startAt(lower)
                    .endAt(lower + "")
                    .limit(20)
                    .get().await()
            }

            snap.documents.mapNotNull { doc ->
                val uid = doc.id
                if (uid == me || uid in myFriendIds) return@mapNotNull null
                Friend(
                    id             = uid,
                    name           = doc.getString("name").orEmpty(),
                    email          = doc.getString("emailMasked").orEmpty(),
                    sharedMemories = 0,
                    isOnline       = false,
                    avatarUrl      = doc.getString("avatarUrl").orEmpty(),
                    bio            = doc.getString("bio").orEmpty()
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * What: Sends a friend request to `targetUid`. Writes both halves of the
     *       request in a single batch:
     *         users/{target}/incomingRequests/{me}  — denormalized sender
     *           card (name + avatar + bio) so the receiver can render the
     *           request row without a second read.
     *         users/{me}/outgoingRequests/{target} — bare {uid, requestedAt}
     *           so our outgoing-requests listener can drive the
     *           "Invitation sent" pill in AddFriendSearch.
     *
     *       The actual friendship only materializes when the target accepts
     *       (see SeedRepository.acceptIncomingRequestsForTestUser for the
     *       symmetric write that flips a request into a friend pair).
     *       Idempotent against double-tap: a uid that already sits in
     *       `outgoingRequests` or `friends` short-circuits.
     */
    suspend fun sendFriendRequest(targetUid: String) {
        val me = AuthRepository.currentUid ?: return
        if (targetUid == me) return
        if (targetUid in _outgoingRequests.value) return
        if (_friends.value.any { it.id == targetUid }) return

        // Read my own card off the public users doc so the receiver's
        // request row has display data without an extra read on their side.
        val mySnap = db.collection("users").document(me).get().await()
        val myName      = mySnap.getString("name").orEmpty()
        val myAvatarUrl = mySnap.getString("avatarUrl").orEmpty()
        val myBio       = mySnap.getString("bio").orEmpty()

        val incomingRef = db.collection("users").document(targetUid)
            .collection("incomingRequests").document(me)
        val outgoingRef = db.collection("users").document(me)
            .collection("outgoingRequests").document(targetUid)

        db.runBatch { batch ->
            batch.set(incomingRef, mapOf(
                "uid"         to me,
                "name"        to myName,
                "avatarUrl"   to myAvatarUrl,
                "bio"         to myBio,
                "status"      to FriendRequest.Status.PENDING.name,
                "requestedAt" to FieldValue.serverTimestamp()
            ))
            batch.set(outgoingRef, mapOf(
                "uid"         to targetUid,
                "requestedAt" to FieldValue.serverTimestamp()
            ))
        }.await()
    }

    private val EmailShape = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /**
     * Symmetrically removes a friendship: deletes both users/{me}/friends/{friendUid}
     * and users/{friendUid}/friends/{me}. Bundled in a Firestore batch so the
     * relationship can never end up half-deleted (one side seeing them as a
     * friend, the other not). The friends listener picks up our side's delete
     * and republishes the list automatically.
     */
    suspend fun deleteFriend(friendUid: String) {
        val me = AuthRepository.currentUid ?: return
        val mySide    = db.collection("users").document(me).collection("friends").document(friendUid)
        val theirSide = db.collection("users").document(friendUid).collection("friends").document(me)
        db.runBatch { batch ->
            batch.delete(mySide)
            batch.delete(theirSide)
        }.await()
    }

    // ── Friend-request actions ──────────────────────────────────────────────
    // requestId == sender's uid == incomingRequests doc id (the same value
    // identifies the request across both subcollections).

    /**
     * What: Promotes a pending request to a real friendship. In one batch:
     *         - writes both halves of the friendship
     *         - flips the receiver's incoming doc `status` to ACCEPTED
     *           (kept around so the receiver's See-all page can still show
     *           the history row — only manual delete removes it)
     *         - deletes the sender's outgoing doc so AddFriendSearch on
     *           their device flips "Invitation sent" → "Added"
     * Who: Called by FriendsViewModel / AllFriendRequestsViewModel.
     * When: User taps the Accept pill on a PENDING row.
     */
    suspend fun accept(requestId: String) {
        val me = AuthRepository.currentUid ?: return
        val senderUid = requestId
        val incomingRef        = db.collection("users").document(me).collection("incomingRequests").document(senderUid)
        val senderOutgoingRef  = db.collection("users").document(senderUid).collection("outgoingRequests").document(me)
        val myFriendRef        = db.collection("users").document(me).collection("friends").document(senderUid)
        val senderFriendRef    = db.collection("users").document(senderUid).collection("friends").document(me)

        db.runBatch { batch ->
            batch.set(myFriendRef, mapOf(
                "uid"   to senderUid,
                "since" to FieldValue.serverTimestamp()
            ))
            batch.set(senderFriendRef, mapOf(
                "uid"   to me,
                "since" to FieldValue.serverTimestamp()
            ))
            batch.update(incomingRef, mapOf(
                "status"     to FriendRequest.Status.ACCEPTED.name,
                "actionedAt" to FieldValue.serverTimestamp()
            ))
            batch.delete(senderOutgoingRef)
        }.await()
        pruneActionedHistory(me)
    }

    /**
     * What: Marks the request as DECLINED on the receiver's side and clears
     *       the sender's outgoing entry. The incoming doc is kept (status
     *       changes to DECLINED) so the user can see who they previously
     *       declined; manual delete is the only way to drop the row.
     */
    suspend fun decline(requestId: String) {
        val me = AuthRepository.currentUid ?: return
        val senderUid = requestId
        val incomingRef       = db.collection("users").document(me).collection("incomingRequests").document(senderUid)
        val senderOutgoingRef = db.collection("users").document(senderUid).collection("outgoingRequests").document(me)

        db.runBatch { batch ->
            batch.update(incomingRef, mapOf(
                "status"     to FriendRequest.Status.DECLINED.name,
                "actionedAt" to FieldValue.serverTimestamp()
            ))
            batch.delete(senderOutgoingRef)
        }.await()
        pruneActionedHistory(me)
    }

    /**
     * Records a friend invitation. No-op if already a friend or already invited.
     * Idempotent so re-tapping "Add" is safe.
     */
    fun invite(userId: String) {
        if (_friends.value.any { it.id == userId }) return
        if (userId in _invitedUserIds.value) return
        _invitedUserIds.value = _invitedUserIds.value + userId
    }

    /**
     * Hard-removes a request entry from the receiver's history. Never touches
     * the friends list — a previously-accepted friend stays friends even if
     * their request row is wiped, and pending/declined entries never produced
     * a friendship so there is nothing to roll back.
     */
    suspend fun deleteRequest(requestId: String) {
        val me = AuthRepository.currentUid ?: return
        db.collection("users").document(me)
            .collection("incomingRequests").document(requestId)
            .delete().await()
    }

    /**
     * What: Trims the actioned-request history (status == ACCEPTED or
     *       DECLINED) on `users/{uid}/incomingRequests` down to the 10
     *       most recent. Storage hygiene — without this, a user who
     *       accepts hundreds of friends over time would accumulate
     *       hundreds of dead docs in their inbox, costing reads on every
     *       open of the Friends tab. Pending docs are never touched.
     *
     *       Called after every accept / decline; one call adds at most
     *       one new actioned doc, so the overflow is at most one. The
     *       prune still scans the whole subcollection to be self-healing
     *       — if the DB drifts above 10 for any reason (legacy data,
     *       partial writes, a missed prune call), the next accept /
     *       decline fixes it.
     *
     *       Sort key is `actionedAt` (when the status flipped), falling
     *       back to `requestedAt` so legacy docs without `actionedAt`
     *       still sort sensibly. Missing both → seconds=0 → end of list,
     *       so those get pruned first.
     */
    private suspend fun pruneActionedHistory(uid: String) {
        val snap = db.collection("users").document(uid)
            .collection("incomingRequests")
            .get().await()

        val actioned = snap.documents.filter {
            val s = it.getString("status").orEmpty().uppercase()
            s == "ACCEPTED" || s == "DECLINED"
        }
        if (actioned.size <= 10) return

        val excess = actioned.sortedByDescending { doc ->
            val ts = doc.getTimestamp("actionedAt") ?: doc.getTimestamp("requestedAt")
            ts?.seconds ?: 0L
        }.drop(10)

        db.runBatch { batch ->
            excess.forEach { batch.delete(it.reference) }
        }.await()
    }

    /**
     * Seeds the demo "discoverable" pool. The real friend / group / request
     * lists come from Firestore via bind().
     */
    private fun seedMockRequestsAndDiscoverable() {
        _discoverableUsers.value = listOf(
            Friend("u_zed",    "Zed",        "zed@gmail.com",            0),
            Friend("u_sonder", "Sonder",     "sonder@gmail.com",         0),
            Friend("u_chieh",  "Chieh",      "chieh@northeastern.edu",   0),
            Friend("u_yeling", "Ye Ling",    "yeling@gmail.com",         0),
            Friend("u_poi",    "Poi",        "poi@outlook.com",          0),
            Friend("u_yj",     "Yang Jugen", "yangjugen@gmail.com",      0),
            Friend("u_happy",  "Happy Day",  "happyday@gmail.com",       0),
            Friend("u_robin",  "Robin Lee",  "robin.lee@gmail.com",      0),
            Friend("u_ivy",    "Ivy Wang",   "ivy.wang@protonmail.com",  0),
            Friend("u_max",    "Max Foster", "max.foster@outlook.com",   0)
        )
    }
}
