package com.cs5520group15.memorycircle.model

/**
 * What: One person who is in the current user's friend list.
 *       Drives every "friend row" / search result on the Friends tab.
 *       avatarUrl is the Firebase Storage download URL of the friend's profile
 *       picture, resolved at read time via AuthRepository.getUserBriefs; blank
 *       means the row falls back to the letter avatar.
 * Who: Used by FriendsRepository and the Friends screens.
 * When: Instantiated whenever the friend list is rebuilt from
 *       users/{uid}/friends + the briefs cache.
 */
data class Friend(
    val id:             String,
    val name:           String,
    val email:          String,
    val sharedMemories: Int,
    val isOnline:       Boolean = false,
    val avatarUrl:      String  = "",
    val bio:            String  = ""
)

/**
 * What: One pending friend request the current user has received from someone
 *       who is NOT yet in their friend list. Carries a tri-state status so the
 *       "See all" page can still show requests after they've been actioned
 *       (greyed accepted, struck-through declined) instead of dropping them silently.
 * Who: Used by FriendsRepository and the Friends screens.
 * When: Instantiated when loading the requests list (mock for now).
 */
data class FriendRequest(
    val id:             String,
    val fromUserId:     String,
    val fromUserName:   String,
    val fromUserEmail:  String,
    val mutualFriends:  Int,
    val status:         Status = Status.PENDING,
    val fromUserBio:    String = ""
) {
    enum class Status { PENDING, ACCEPTED, DECLINED }
}

/**
 * What: A lightweight group reference used to render the Groups tab on the
 *       Friends screen and the group search results. The header row needs
 *       title + member count; the Friends-tab row also renders a WeChat-style
 *       3×3 avatar collage from `memberAvatarUrls` (up to 9 entries, with the
 *       rest dropping into an "and N more" affordance handled by the row).
 *       Search rows ignore the collage.
 * Who: Used by FriendsRepository, FriendsSearchViewModel, and their screens.
 * When: Instantiated whenever the user's group list is rebuilt from
 *       `groups whereArrayContains memberIds` + the briefs cache.
 */
data class GroupSummary(
    val id:                String,
    val name:              String,
    val memberCount:       Int,
    val memberAvatarUrls:  List<String> = emptyList(),
    val memberNames:       List<String> = emptyList()
)
