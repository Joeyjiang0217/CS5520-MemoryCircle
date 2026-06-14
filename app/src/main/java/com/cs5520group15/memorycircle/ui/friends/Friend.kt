package com.cs5520group15.memorycircle.ui.friends

/**
 * What: One person who is in the current user's friend list.
 *       Drives every "friend row" / search result on the Friends tab.
 * Who: Used by FriendsRepository and the Friends screens.
 * When: Instantiated when loading the friend list (mock for now, Firestore later).
 */
data class Friend(
    val id:             String,
    val name:           String,
    val email:          String,
    val sharedMemories: Int,
    val isOnline:       Boolean = false
)

/**
 * What: One pending friend request the current user has received from someone
 *       who is NOT yet in their friend list. Carries a tri-state status so the
 *       "See all" page can still show requests after they've been actioned
 *       (greyed / struck-through) instead of dropping them silently.
 * Who: Used by FriendsRepository and the Friends screens.
 * When: Instantiated when loading the requests list (mock for now).
 */
data class FriendRequest(
    val id:             String,
    val fromUserId:     String,
    val fromUserName:   String,
    val fromUserEmail:  String,
    val mutualFriends:  Int,
    val status:         Status = Status.PENDING
) {
    enum class Status { PENDING, ACCEPTED, DECLINED }
}

/**
 * What: A lightweight group reference used to render group search results without
 *       pulling in HomeViewModel's richer shape. Title + member count is all the
 *       search row needs; tapping it navigates to GroupDetail by id.
 * Who: Used by FriendsSearchViewModel and FriendsSearchScreen.
 * When: Instantiated when seeding searchable groups (mock for now).
 */
data class GroupSummary(
    val id:          String,
    val name:        String,
    val memberCount: Int
)
