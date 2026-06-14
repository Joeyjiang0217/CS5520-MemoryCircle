package com.cs5520group15.memorycircle.ui.friends

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: State for the full-screen friend/group search page. Holds the current
 *       text query and exposes case-insensitive substring matches over a mock
 *       friend pool (name + email) and a mock group pool (name). Reads/writes
 *       the recent-search history through FriendsSearchRepository.
 * Who: Used by FriendsSearchScreen.
 * When: Created when the search screen first composes; survives config changes.
 *       Firestore will replace the mock seeds later.
 */
class FriendsSearchViewModel : ViewModel() {

    private val _query   = MutableStateFlow("")
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    private val _groups  = MutableStateFlow<List<GroupSummary>>(emptyList())

    val query:   StateFlow<String>             = _query.asStateFlow()
    val friends: StateFlow<List<Friend>>       = _friends.asStateFlow()
    val groups:  StateFlow<List<GroupSummary>> = _groups.asStateFlow()
    val recent:  StateFlow<List<String>>       = FriendsSearchRepository.recent

    init { loadMock() }

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

    /**
     * What: Case-insensitive substring filter against name OR email.
     * Who: Called by FriendsSearchScreen to derive the visible friend results.
     * When: Every recomposition while the query is non-blank.
     */
    fun matchFriends(q: String): List<Friend> {
        val needle = q.trim()
        if (needle.isEmpty()) return emptyList()
        return _friends.value.filter { f ->
            f.name.contains(needle, ignoreCase = true) ||
            f.email.contains(needle, ignoreCase = true)
        }
    }

    /**
     * What: Case-insensitive substring filter against the group name.
     * Who: Called by FriendsSearchScreen to derive the visible group results.
     * When: Every recomposition while the query is non-blank.
     */
    fun matchGroups(q: String): List<GroupSummary> {
        val needle = q.trim()
        if (needle.isEmpty()) return emptyList()
        return _groups.value.filter { it.name.contains(needle, ignoreCase = true) }
    }

    private fun loadMock() {
        // Reuse the same names that appear on Home / GroupDetail / Friends so the
        // demo stays coherent across screens.
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
        _groups.value = listOf(
            GroupSummary("1", "Weekend Crew",  5),
            GroupSummary("2", "Family Circle", 3),
            GroupSummary("3", "Travel Buddies", 6)
        )
    }
}
