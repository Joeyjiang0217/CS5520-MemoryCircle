package com.cs5520group15.memorycircle.ui.friends

import androidx.lifecycle.ViewModel
import com.cs5520group15.memorycircle.data.FriendsRepository

/**
 * What: Thin wrapper exposing the shared FriendsRepository state to the Friends
 *       landing tab. Mutation methods just forward to the repository so the
 *       changes are immediately visible on every screen that reads the same
 *       source (FriendsScreen, AllFriendRequestsScreen, FriendsSearchScreen).
 * Who: Used by FriendsScreen.
 * When: Created on first composition; survives config changes.
 */
class FriendsViewModel : ViewModel() {

    val friends  = FriendsRepository.friends
    val groups   = FriendsRepository.groups
    val requests = FriendsRepository.requests  // full list (incl. accepted/declined)

    fun acceptRequest(id: String) = FriendsRepository.accept(id)
    fun rejectRequest(id: String) = FriendsRepository.decline(id)
}
