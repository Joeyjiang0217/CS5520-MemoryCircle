package com.cs5520group15.memorycircle.ui.friendrequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.FriendsRepository
import com.cs5520group15.memorycircle.model.FriendRequest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * What: Thin wrapper exposing the requests list (capped at the 10 most-recent
 *       by the repository's Firestore query) and the three actions the
 *       "See all" screen needs (accept / decline / delete). Sits between the
 *       Composable and FriendsRepository so the screen never touches the
 *       data source directly. Mutations all delegate to the repository (the
 *       single source of truth), wrapped in viewModelScope so the suspend
 *       calls don't block the UI thread.
 *
 *       Idempotent FriendsRepository.bind() in init means navigating to
 *       this screen without first visiting the Friends tab still warms up
 *       the incomingRequests listener.
 * Who: Used by AllFriendRequestsScreen.
 * When: Created on first composition; survives config changes.
 */
class AllFriendRequestsViewModel : ViewModel() {

    init { FriendsRepository.bind() }

    val requests: StateFlow<List<FriendRequest>> = FriendsRepository.requests

    fun accept(id: String) = viewModelScope.launch {
        runCatching { FriendsRepository.accept(id) }
    }

    fun decline(id: String) = viewModelScope.launch {
        runCatching { FriendsRepository.decline(id) }
    }

    /**
     * What: Drops the request entry from the list. INVARIANT: never touches the
     *       friends list — see FriendsRepository.deleteRequest. A previously
     *       accepted friend stays a friend even if their request history row
     *       is wiped; pending/declined requests never produced a friend so
     *       there is nothing to roll back.
     * Who: Called by AllFriendRequestsScreen on confirmed swipe-to-delete.
     * When: Per confirmed delete action.
     */
    fun delete(id: String) = viewModelScope.launch {
        runCatching { FriendsRepository.deleteRequest(id) }
    }
}
