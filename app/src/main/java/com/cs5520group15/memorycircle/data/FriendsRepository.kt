package com.cs5520group15.memorycircle.data

import com.cs5520group15.memorycircle.model.Friend
import com.cs5520group15.memorycircle.model.FriendRequest
import com.cs5520group15.memorycircle.model.GroupSummary
import com.google.firebase.firestore.ListenerRegistration
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
 *       Friend requests, the search "discoverable pool", and the locally-sent
 *       invite set still live in mock — Firestore wiring for those lands in
 *       the next turn.
 *
 * Who: Used by FriendsViewModel, FriendsSearchViewModel, and
 *      AllFriendRequestsScreen.
 * When: bind() is called from FriendsViewModel.init; listeners detach on
 *       logout / when no user is signed in.
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
        userListeners.values.forEach { it.remove() }
        userListeners.clear()
        userCards.clear()
        lastFriendUids        = emptyList()
        lastGroups            = emptyList()
        _outgoingRequests.value = emptySet()
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
                "requestedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))
            batch.set(outgoingRef, mapOf(
                "uid"         to targetUid,
                "requestedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
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
    // Still mock-backed; Firestore wiring for requests lands next turn.

    /**
     * What: Marks a pending request as ACCEPTED and appends the sender to the
     *       friend list. No-op if the request is missing or already actioned.
     */
    fun accept(requestId: String) {
        val req = _requests.value.firstOrNull { it.id == requestId } ?: return
        if (req.status != FriendRequest.Status.PENDING) return
        _requests.value = _requests.value.map {
            if (it.id == requestId) it.copy(status = FriendRequest.Status.ACCEPTED) else it
        }
        if (_friends.value.none { it.id == req.fromUserId }) {
            _friends.value = _friends.value + Friend(
                id             = req.fromUserId,
                name           = req.fromUserName,
                email          = req.fromUserEmail,
                sharedMemories = 0
            )
        }
    }

    /** Marks a pending request as DECLINED; stays in the list for the See-all page. */
    fun decline(requestId: String) {
        _requests.value = _requests.value.map {
            if (it.id == requestId && it.status == FriendRequest.Status.PENDING)
                it.copy(status = FriendRequest.Status.DECLINED)
            else it
        }
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

    /** Hard-removes a request entry. Never touches the friends list. */
    fun deleteRequest(requestId: String) {
        _requests.value = _requests.value.filterNot { it.id == requestId }
    }

    /**
     * Seeds the still-mock request queue and the demo "discoverable" pool so
     * the friend-search / requests UIs have something to render against. The
     * real friend list and group list come from Firestore via bind().
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

        _requests.value = listOf(
            FriendRequest("r1", "u_alex", "Alex Park",   "alex.park@gmail.com",   2),
            FriendRequest("r2", "u_dan",  "Dan Patel",   "dan.patel@gmail.com",   0),
            FriendRequest("r3", "u_mei",  "Mei Tanaka",  "mei.tanaka@gmail.com",  1),
            FriendRequest("r4", "u_sam",  "Sam Rivera",  "sam.rivera@gmail.com",  3),
            FriendRequest("r5", "u_ada",  "Ada Okafor",  "ada.okafor@gmail.com",  0)
        )
    }
}
