package com.cs5520group15.memorycircle.ui.friendrequests

import androidx.lifecycle.ViewModel
import com.cs5520group15.memorycircle.data.FriendsRepository
import com.cs5520group15.memorycircle.model.FriendRequest
import kotlinx.coroutines.flow.StateFlow

/**
 * What: Thin wrapper exposing the requests list and the three actions the
 *       "See all" screen needs (accept / decline / delete). Sits between the
 *       Composable and FriendsRepository so the screen never touches the data
 *       source directly — easier to swap to Firestore later, easier to test.
 *       Mutations all delegate to the repository (the single source of truth).
 * Who: Used by AllFriendRequestsScreen.
 * When: Created on first composition; survives config changes.
 */
class AllFriendRequestsViewModel : ViewModel() {

    val requests: StateFlow<List<FriendRequest>> = FriendsRepository.requests

    fun accept(id: String) = FriendsRepository.accept(id)

    fun decline(id: String) = FriendsRepository.decline(id)

    /**
     * What: Drops the request entry from the list. INVARIANT: never touches the
     *       friends list — see FriendsRepository.deleteRequest. A previously
     *       accepted friend stays a friend even if their request history row
     *       is wiped; pending/declined requests never produced a friend so
     *       there is nothing to roll back.
     * Who: Called by AllFriendRequestsScreen on confirmed swipe-to-delete.
     * When: Per confirmed delete action.
     */
    fun delete(id: String) = FriendsRepository.deleteRequest(id)
}
