package com.cs5520group15.memorycircle.ui.friends

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: In-memory store for the user's last N search queries on the Friends
 *       search page. Newer entries push older ones out; duplicates (case-insensitive)
 *       move to the top instead of stacking. Survives navigation but resets on
 *       app restart — Firestore / DataStore will replace it later.
 * Who: Used by FriendsSearchViewModel (reads + writes).
 * When: Read whenever the search page is shown; written when the user taps a
 *       result, committing the current query to history.
 */
object FriendsSearchRepository {

    private const val MAX_RECENT = 10

    private val _recent = MutableStateFlow<List<String>>(emptyList())

    val recent: StateFlow<List<String>> = _recent.asStateFlow()

    /**
     * What: Commits a query to the recent-search list. Blank queries are ignored;
     *       case-insensitive duplicates are deduped and re-inserted at the top.
     *       The list is capped at MAX_RECENT (oldest entries fall off).
     * Who: Called by FriendsSearchViewModel when the user taps a search result.
     * When: On every result tap with a non-blank query.
     */
    fun addRecent(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val withoutDup = _recent.value.filterNot { it.equals(trimmed, ignoreCase = true) }
        _recent.value = (listOf(trimmed) + withoutDup).take(MAX_RECENT)
    }
}
