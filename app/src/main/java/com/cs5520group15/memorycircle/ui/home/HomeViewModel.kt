package com.cs5520group15.memorycircle.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: Holds all UI state for the Home screen.
 * Who: Used by HomeScreen.
 * When: Created when HomeScreen is first displayed, survives configuration changes.
 */
class HomeViewModel : ViewModel() {

    // --- Data Models ---

    /**
     * What: Represents a single memory group card displayed on the Home screen.
     * Who: Used by HomeViewModel and HomeScreen.
     * When: Instantiated when loading the list of recent groups.
     */
    data class Group(
        val id:          String,
        val name:        String,
        val date:        String,
        val memoryCount: Int,
        val colorType:   String  // "brown" or "sage" — controls card gradient color
    )

    // Private mutable state — only ViewModel can change
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    private val _userName = MutableStateFlow("Sarah")

    // Public read-only state — UI observes these
    val groups:    StateFlow<List<Group>> = _groups.asStateFlow()
    val userName:  StateFlow<String>      = _userName.asStateFlow()

    // Load dummy data when ViewModel is created
    // Firebase will replace this in a later phase
    init {
        loadDummyGroups()
    }

    /**
     * What: Loads a hardcoded list of groups for UI skeleton demonstration.
     * Who: Called automatically in the init block.
     * When: Once when the ViewModel is first created.
     */
    private fun loadDummyGroups() {
        _groups.value = listOf(
            Group("1", "Summer Picnic",  "June 1, 2025",  12, "brown"),
            Group("2", "Garden Walk",    "May 28, 2025",   5, "sage"),
            Group("3", "Beach Trip",     "May 10, 2025",   8, "brown")
        )
    }
}