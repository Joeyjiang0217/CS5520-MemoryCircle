/**
 * What: ViewModel that holds UI state and business logic for the Friends screen.
 * Who:  Used by FriendsScreen.
 * When: Created when FriendsScreen is first composed; survives config changes.
 */

package com.cs5520group15.memorycircle.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.FriendsRepository
import kotlinx.coroutines.launch

/**
 * What: Thin wrapper exposing the shared FriendsRepository state to the Friends
 *       landing tab. Mutation methods forward to the repository so changes are
 *       visible across every screen reading the same source. The init block
 *       triggers FriendsRepository.bind() to attach the Firestore listeners
 *       for the current user's friends + groups.
 * Who: Used by FriendsScreen.
 * When: Created on first composition; survives config changes.
 */
class FriendsViewModel : ViewModel() {

    init { FriendsRepository.bind() }

    val friends  = FriendsRepository.friends
    val groups   = FriendsRepository.groups
    val requests = FriendsRepository.requests  // full list (incl. accepted/declined)

    fun acceptRequest(id: String) = viewModelScope.launch {
        runCatching { FriendsRepository.accept(id) }
    }
    fun rejectRequest(id: String) = viewModelScope.launch {
        runCatching { FriendsRepository.decline(id) }
    }

    /**
     * Symmetrically removes a friendship. The Firestore listener picks up the
     * delete and republishes the friend list, so the row disappears without an
     * explicit local mutation.
     */
    fun deleteFriend(friendUid: String) = viewModelScope.launch {
        runCatching { FriendsRepository.deleteFriend(friendUid) }
    }
}
