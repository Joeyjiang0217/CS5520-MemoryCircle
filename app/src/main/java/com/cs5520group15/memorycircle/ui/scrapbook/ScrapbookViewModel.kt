package com.cs5520group15.memorycircle.ui.scrapbook

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: Holds the UI state for the Scrapbook creation screen.
 * Who: Used by ScrapbookScreen.
 * When: Created when the creation screen is displayed.
 */
class ScrapbookViewModel : ViewModel() {

    /**
     * What: A group the user can pick as the source for a new scrapbook.
     * Who: Used by ScrapbookViewModel and ScrapbookScreen.
     * When: Listed in the "Select group" section of the creation screen.
     */
    data class GroupOption(
        val id:   String,
        val name: String
    )

    // Existing groups the user can choose from — Firebase will replace this later.
    // Mirrors the dummy groups shown on the Home screen.
    val availableGroups = listOf(
        GroupOption("1", "Group 1"),
        GroupOption("2", "Group 2"),
        GroupOption("3", "Group 3")
    )

    // Group-size options. A scrapbook shows one photo per member at each date,
    // so this count drives the generated layout (how many member photos per date).
    val availableMemberCounts = listOf(2, 3, 4, 5, 6)

    // --- State ---
    private val _selectedGroupId     = MutableStateFlow<String?>(null)
    private val _selectedMemberCount = MutableStateFlow(4)
    private val _journalEntry        = MutableStateFlow("")
    private val _tags                = MutableStateFlow(listOf("#2026", "#friends"))

    val selectedGroupId:     StateFlow<String?>      = _selectedGroupId.asStateFlow()
    val selectedMemberCount: StateFlow<Int>          = _selectedMemberCount.asStateFlow()
    val journalEntry:        StateFlow<String>       = _journalEntry.asStateFlow()
    val tags:                StateFlow<List<String>> = _tags.asStateFlow()

    /**
     * What: Selects a single group as the source for the scrapbook.
     *       Tapping the already-selected group clears the selection.
     * Who: Called by ScrapbookScreen when user taps a group chip.
     * When: On group selection.
     */
    fun onSelectGroup(groupId: String) {
        _selectedGroupId.value = if (_selectedGroupId.value == groupId) null else groupId
    }

    /**
     * What: Sets the group size (number of members) for the scrapbook.
     * Who: Called by ScrapbookScreen when user taps a group-size chip.
     * When: On group-size selection.
     */
    fun onMemberCountSelected(count: Int) {
        _selectedMemberCount.value = count
    }

    /**
     * What: Updates the journal entry text.
     * Who: Called by ScrapbookScreen's journal text field.
     * When: Every time the user types a character.
     */
    fun onJournalChange(value: String) {
        _journalEntry.value = value
    }

    /**
     * What: Adds a new tag to the tags list.
     * Who: Called by ScrapbookScreen when user taps "+ Add tag".
     * When: On tag addition.
     */
    fun onAddTag(tag: String) {
        if (tag.isBlank()) return
        _tags.value = _tags.value + "#$tag"
    }

    /**
     * What: Removes a tag from the tags list.
     * Who: Called by ScrapbookScreen when user taps a tag chip.
     * When: On tag removal.
     */
    fun onRemoveTag(tag: String) {
        _tags.value = _tags.value.filter { it != tag }
    }
}
