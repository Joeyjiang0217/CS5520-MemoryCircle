/**
 * What: ViewModel that holds UI state and business logic for the Friends Search
 *       screen.
 * Who:  Used by FriendsSearchScreen.
 * When: Created when FriendsSearchScreen is first composed; survives config
 *       changes.
 */

package com.cs5520group15.memorycircle.ui.friendsearch

import androidx.lifecycle.ViewModel
import com.cs5520group15.memorycircle.data.FriendsRepository
import com.cs5520group15.memorycircle.data.FriendsSearchRepository
import com.cs5520group15.memorycircle.model.Friend
import com.cs5520group15.memorycircle.model.GroupSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: State for the full-screen friend/group search page. Holds the current
 *       text query and exposes case-insensitive substring matches over the
 *       user's real friend list and group list (both already live Firestore
 *       subscriptions on FriendsRepository). Reads/writes recent-search
 *       history through FriendsSearchRepository.
 *
 *       No network gate here: the friend/group lists are already cached
 *       locally by Firestore offline persistence, so this screen works
 *       offline. The add-new-friend search (a different surface) is the one
 *       that needs connectivity.
 * Who: Used by FriendsSearchScreen.
 * When: Created when the search screen first composes; survives config
 *       changes.
 */
class FriendsSearchViewModel : ViewModel() {

    // Idempotent — if the Friends tab already drove bind() this is a no-op.
    init { FriendsRepository.bind() }

    private val _query = MutableStateFlow("")

    val query:   StateFlow<String>             = _query.asStateFlow()
    val friends: StateFlow<List<Friend>>       = FriendsRepository.friends
    val groups:  StateFlow<List<GroupSummary>> = FriendsRepository.groups
    val recent:  StateFlow<List<String>>       = FriendsSearchRepository.recent

    fun onQueryChange(s: String) { _query.value = s }

    /**
     * What: Commits the current query to recent-search history. No-op for blank
     *       queries (handled by the repository).
     * Who: Called by FriendsSearchScreen when the user taps a search result.
     * When: On every result tap.
     */
    fun commitQueryToHistory() {
        FriendsSearchRepository.addRecent(_query.value)
    }

    fun matchFriends(q: String): List<Friend> {
        val needle = q.trim()
        if (needle.isEmpty()) return emptyList()
        return friends.value.filter { f ->
            f.name.contains(needle, ignoreCase = true) ||
            f.email.contains(needle, ignoreCase = true)
        }
    }

    fun matchGroups(q: String): List<GroupSummary> {
        val needle = q.trim()
        if (needle.isEmpty()) return emptyList()
        return groups.value.filter { it.name.contains(needle, ignoreCase = true) }
    }
}
