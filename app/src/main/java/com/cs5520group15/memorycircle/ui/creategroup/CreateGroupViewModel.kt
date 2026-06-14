package com.cs5520group15.memorycircle.ui.creategroup

import androidx.lifecycle.ViewModel
import com.cs5520group15.memorycircle.data.FriendsRepository
import com.cs5520group15.memorycircle.data.ScrapbookRepository
import com.cs5520group15.memorycircle.model.Friend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: Drives the WeChat-style "create a new group" contact picker. Holds the
 *       set of selected member ids and the live search query; exposes the
 *       friend pool from FriendsRepository so the rows the user picks from
 *       stay in sync with the rest of the app.
 *
 *       Selection state is `Set<String>` rather than `List<String>` so toggling
 *       is O(log n) and idempotent — re-tapping a row that's already selected
 *       removes it, which is the universal contact-picker convention.
 * Who: Used by CreateGroupScreen.
 * When: Created when the screen first composes; discarded when the user
 *       confirms (the screen pops off the back stack via popUpTo inclusive).
 */
class CreateGroupViewModel : ViewModel() {

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _query       = MutableStateFlow("")

    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()
    val query:       StateFlow<String>      = _query.asStateFlow()

    /** Source of contacts the picker offers — the user's existing friend list. */
    val contacts: StateFlow<List<Friend>> = FriendsRepository.friends

    /**
     * What: Flips the membership of `userId` in the selected set. No-op aside
     *       from the toggle — the screen reads `selectedIds` directly to render
     *       the checkbox state.
     * Who: Called when the user taps a contact row or its checkbox.
     */
    fun toggle(userId: String) {
        val current = _selectedIds.value
        _selectedIds.value = if (userId in current) current - userId else current + userId
    }

    fun onQueryChange(value: String) { _query.value = value }

    /** Clears the active search query. Called when the user picks a result or
     *  taps the X clear affordance inside the search bar. */
    fun clearQuery() { _query.value = "" }

    /**
     * What: Case-insensitive substring filter against name OR email. Empty /
     *       blank queries return an empty list so the caller can simply check
     *       isEmpty() to decide whether to render the results section.
     * Who: Called by CreateGroupScreen while the active-search TextField has
     *      a non-blank value.
     * When: Every recomposition with a non-blank query.
     */
    fun match(query: String): List<Friend> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        return contacts.value.filter { c ->
            c.name.contains(needle, ignoreCase = true) ||
            c.email.contains(needle, ignoreCase = true)
        }
    }

    /**
     * What: Mints a new group id, registers an empty scrapbook timeline for it
     *       (so the viewer doesn't lazily seed it with mock entries the way
     *       every other group is seeded), and returns the id for the caller
     *       to navigate with. No-op + returns null if no member is selected —
     *       the UI prevents this but the guard keeps the contract honest.
     * Who: Called by CreateGroupScreen when the user taps "Create Now".
     * When: On Create-button click, only enabled when `selectedIds` is non-empty.
     */
    fun createGroup(): String? {
        if (_selectedIds.value.isEmpty()) return null
        val newId = "g${System.currentTimeMillis()}"
        ScrapbookRepository.createEmptyGroup(newId)
        return newId
    }
}
