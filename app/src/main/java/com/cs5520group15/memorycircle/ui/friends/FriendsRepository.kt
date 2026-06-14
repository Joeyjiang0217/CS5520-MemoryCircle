package com.cs5520group15.memorycircle.ui.friends

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: In-memory single source of truth for the current user's friends, the
 *       friend-request queue, and the group count shown on the Friends header.
 *       Friend requests are STATEFUL — accepting/declining mutates the status
 *       rather than removing the entry, so the "See all" page can show history
 *       (greyed accepted, struck-through declined). Hard delete only happens
 *       on swipe-to-dismiss confirmation.
 *       Survives navigation but resets on app restart — Firestore later.
 * Who: Used by FriendsViewModel, FriendsSearchViewModel, and
 *       AllFriendRequestsScreen.
 * When: Read on every Friends-tab composition; written on accept / decline /
 *       delete actions, and when search seeds groups (later).
 */
object FriendsRepository {

    private val _friends           = MutableStateFlow<List<Friend>>(emptyList())
    private val _requests          = MutableStateFlow<List<FriendRequest>>(emptyList())
    private val _groups            = MutableStateFlow<List<GroupSummary>>(emptyList())
    private val _discoverableUsers = MutableStateFlow<List<Friend>>(emptyList())
    private val _invitedUserIds    = MutableStateFlow<Set<String>>(emptySet())

    val friends:           StateFlow<List<Friend>>        = _friends.asStateFlow()
    val requests:          StateFlow<List<FriendRequest>> = _requests.asStateFlow()
    val groups:            StateFlow<List<GroupSummary>>  = _groups.asStateFlow()

    /**
     * What: Broader pool of users the current user can find via "add new friend"
     *       search — INCLUDES current friends (they render with an "Added" pill
     *       so the user knows not to invite again) plus strangers seeded for
     *       demo purposes. Reuses the Friend type for shape compatibility;
     *       stranger entries simply have sharedMemories = 0 and isOnline = false.
     */
    val discoverableUsers: StateFlow<List<Friend>>        = _discoverableUsers.asStateFlow()

    /**
     * What: IDs of users the current user has sent a friend invitation to but
     *       who have not yet been added to the friend list. Drives the
     *       "Invitation sent" locked pill on AddFriendSearchScreen.
     */
    val invitedUserIds:    StateFlow<Set<String>>         = _invitedUserIds.asStateFlow()

    init { seedMock() }

    /**
     * What: Marks a pending request as ACCEPTED and appends the sender to the
     *       friend list. No-op if the request is missing or already actioned.
     * Who: Called by FriendsViewModel and AllFriendRequestsScreen.
     * When: On Accept tap.
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

    /**
     * What: Marks a pending request as DECLINED. The row stays in the list so
     *       the "See all" page can render it as struck-through; the user must
     *       swipe-to-dismiss to actually remove it.
     * Who: Called by FriendsViewModel and AllFriendRequestsScreen.
     * When: On Decline (×) tap.
     */
    fun decline(requestId: String) {
        _requests.value = _requests.value.map {
            if (it.id == requestId && it.status == FriendRequest.Status.PENDING)
                it.copy(status = FriendRequest.Status.DECLINED)
            else it
        }
    }

    /**
     * What: Records that the current user has sent a friend invitation to the
     *       given userId. No-op if they are already a friend (nothing to invite)
     *       or have already been invited (idempotent — keeps re-tapping "Add"
     *       safe). The "Add new friend" search screen reads invitedUserIds to
     *       decide whether to render Add / Invitation sent / Added pills.
     * Who: Called by AddFriendSearchViewModel.invite.
     * When: On every tap of the Add button on a search result.
     */
    fun invite(userId: String) {
        if (_friends.value.any { it.id == userId }) return
        if (userId in _invitedUserIds.value) return
        _invitedUserIds.value = _invitedUserIds.value + userId
    }

    /**
     * What: Hard-removes a request entry from the requests list. INVARIANT:
     *       never touches the friends list. An already-befriended user stays
     *       a friend even when their accepted-request row is wiped from
     *       history; pending and declined entries never produced a friend in
     *       the first place, so there is nothing to roll back either.
     *       Used by the swipe-to-dismiss confirmation on AllFriendRequestsScreen.
     * Who: Called by AllFriendRequestsViewModel.
     * When: On confirmed swipe-to-delete.
     */
    fun deleteRequest(requestId: String) {
        // NOTE: do NOT touch _friends here. See KDoc invariant above.
        _requests.value = _requests.value.filterNot { it.id == requestId }
    }

    private fun seedMock() {
        _friends.value = listOf(
            Friend("u_emma",  "Emma Wilson",  "emma.wilson@gmail.com",         34, isOnline = true),
            Friend("u_james", "James Liu",    "james.liu@northeastern.edu",    21),
            Friend("u_mia",   "Mia Torres",   "mia.torres@protonmail.com",     18),
            Friend("u_lila",  "Lila Nguyen",  "lila.nguyen@hotmail.com",        9),
            Friend("u_kai",   "Kai Nakamura", "kai.nakamura@gmail.com",         7),
            Friend("u_zoe",   "Zoe Martin",   "zoe.martin@yahoo.com",          11),
            Friend("u_noah",  "Noah Bennett", "noah.bennett@outlook.com",       6),
            Friend("u_riya",  "Riya Patel",   "riya.patel@gmail.com",           4),
            Friend("u_dad",   "David Chen",   "david.chen@gmail.com",          30),
            Friend("u_mom",   "Helen Chen",   "helen.chen@gmail.com",          27),
            Friend("u_leo",   "Leo Park",     "leo.park@gmail.com",            12),
            Friend("u_isla",  "Isla Hughes",  "isla.hughes@northeastern.edu",   3)
        )
        // Mock pool the "Add new friend" search runs against. Includes the
        // current friends so the screen can show "Added" pills for them, plus
        // a handful of strangers reachable by name or email.
        _discoverableUsers.value = _friends.value + listOf(
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

        // Mock groups — same shape used by FriendsSearchViewModel so the demo
        // stays coherent across the search results and the group tab.
        _groups.value = listOf(
            GroupSummary("1", "Weekend Crew",   5),
            GroupSummary("2", "Family Circle",  3),
            GroupSummary("3", "Travel Buddies", 6)
        )
        // Seed a handful of pending requests, all PENDING by default.
        _requests.value = listOf(
            FriendRequest("r1", "u_alex", "Alex Park",   "alex.park@gmail.com",   2),
            FriendRequest("r2", "u_dan",  "Dan Patel",   "dan.patel@gmail.com",   0),
            FriendRequest("r3", "u_mei",  "Mei Tanaka",  "mei.tanaka@gmail.com",  1),
            FriendRequest("r4", "u_sam",  "Sam Rivera",  "sam.rivera@gmail.com",  3),
            FriendRequest("r5", "u_ada",  "Ada Okafor",  "ada.okafor@gmail.com",  0)
        )
    }
}
