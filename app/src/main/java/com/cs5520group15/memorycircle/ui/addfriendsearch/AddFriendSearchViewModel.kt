/**
 * What: ViewModel that holds UI state and business logic for the Add Friend
 *       Search screen.
 * Who:  Used by AddFriendSearchScreen.
 * When: Created when AddFriendSearchScreen is first composed; survives config
 *       changes.
 */

package com.cs5520group15.memorycircle.ui.addfriendsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.FriendsRepository
import com.cs5520group15.memorycircle.model.Friend
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * What: State for the "Add new friend" search overlay. Holds the live `query`
 *       (what the user is currently typing), the committed `submittedQuery`
 *       (last query the user pressed Search on), and the search results
 *       loaded from Firestore.
 *
 *       Matching logic (server-side, via FriendsRepository.searchUsers):
 *         - Full email (matches EmailShape regex) → exact-match on the public
 *           emailMasked field (we mask the input the same way registration
 *           masks the email, then equal-match).
 *         - Plain text → name prefix query (Firestore's natural sort), capped
 *           at 20 results. Case-sensitive on the server side; this is the
 *           same limitation any prefix index has without a normalized
 *           nameLower field.
 *
 *       Self + existing friends are filtered out server-side so the result
 *       list only ever shows people the user can actually add.
 * Who: Used by AddFriendSearchScreen.
 * When: Created on first composition; survives config changes.
 */
class AddFriendSearchViewModel : ViewModel() {

    // FriendsRepository.bind() so we have a live friend list to dedupe
    // against; idempotent if FriendsViewModel already bound earlier.
    init { FriendsRepository.bind() }

    private val _query          = MutableStateFlow("")
    private val _submittedQuery = MutableStateFlow("")
    private val _results        = MutableStateFlow<List<Friend>>(emptyList())
    private val _isSearching    = MutableStateFlow(false)
    private val _isAdding       = MutableStateFlow<Set<String>>(emptySet())

    val query:          StateFlow<String>       = _query.asStateFlow()
    val submittedQuery: StateFlow<String>       = _submittedQuery.asStateFlow()
    val results:        StateFlow<List<Friend>> = _results.asStateFlow()
    val isSearching:    StateFlow<Boolean>      = _isSearching.asStateFlow()
    /** uids whose Add-friend write is currently in flight. The row's pill
     *  reads as locked while the uid is in this set. */
    val isAdding:       StateFlow<Set<String>>  = _isAdding.asStateFlow()

    val friends:          StateFlow<List<Friend>>  = FriendsRepository.friends
    val outgoingRequests: StateFlow<Set<String>>   = FriendsRepository.outgoingRequests

    sealed class AddFriendEvent {
        data class ShowSnackbar(val message: String) : AddFriendEvent()
    }
    private val _events = Channel<AddFriendEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onQueryChange(s: String) { _query.value = s }

    /**
     * Commits the current live query and fires the Firestore search. The
     * screen layer is responsible for the offline pre-check so we never get
     * here without network; we still emit a snackbar on Firestore errors.
     */
    fun submit() {
        val q = _query.value.trim()
        _submittedQuery.value = q
        if (q.isEmpty()) {
            _results.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            _results.value = runCatching { FriendsRepository.searchUsers(q) }
                .getOrElse {
                    _events.send(AddFriendEvent.ShowSnackbar(it.message ?: "Search failed"))
                    emptyList()
                }
            _isSearching.value = false
        }
    }

    /**
     * Sends a friend request via FriendsRepository.sendFriendRequest. Once
     * the write succeeds the outgoingRequests listener fires and the
     * recipient's uid lands in `outgoingRequests`, flipping the row's pill
     * to "Invitation sent" via the screen-level membership check. While the
     * write is in flight the uid sits in `_isAdding` so the pill is locked
     * showing "Sending..." instead.
     */
    fun sendFriendRequest(userId: String) = viewModelScope.launch {
        if (userId in _isAdding.value) return@launch
        _isAdding.value = _isAdding.value + userId
        runCatching { FriendsRepository.sendFriendRequest(userId) }
            .onFailure { _events.send(AddFriendEvent.ShowSnackbar(it.message ?: "Failed to send request")) }
        _isAdding.value = _isAdding.value - userId
    }
}
