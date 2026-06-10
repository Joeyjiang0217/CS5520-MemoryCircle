package com.cs5520group15.memorycircle.ui.memories

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: Holds the UI state for the Memories (calendar) screen.
 *       Scrapbooks are generated per month, so the data is grouped by month.
 *       Each month holds the scrapbooks created for that month (one per group).
 * Who: Used by MemoriesScreen.
 * When: Created when MemoriesScreen is first displayed, survives config changes.
 */
class MemoriesViewModel : ViewModel() {

    /**
     * What: A single generated scrapbook belonging to one group within a month.
     * Who: Used by MemoriesViewModel and MemoriesScreen.
     * When: Instantiated when loading the month sections.
     */
    data class Scrapbook(
        val id:          String,
        val groupId:     String,
        val groupName:   String,  // e.g. "Group 1"
        val memoryCount: Int,
        val colorType:   String   // "brown" or "sage" — controls card accent color
    )

    /**
     * What: One month bucket in the calendar, holding all scrapbooks for that month.
     * Who: Used by MemoriesViewModel and MemoriesScreen.
     * When: Instantiated when loading dummy data.
     */
    data class MonthSection(
        val month:      String,  // e.g. "January"
        val year:       String,  // e.g. "2025"
        val scrapbooks: List<Scrapbook>
    )

    private val _months = MutableStateFlow<List<MonthSection>>(emptyList())
    val months: StateFlow<List<MonthSection>> = _months.asStateFlow()

    init {
        loadDummyMonths()
    }

    /**
     * What: Loads a hardcoded calendar of months with their scrapbooks.
     *       Firebase will replace this in a later phase.
     * Who: Called automatically in the init block.
     * When: Once when the ViewModel is first created.
     */
    private fun loadDummyMonths() {
        _months.value = listOf(
            MonthSection(
                month = "March", year = "2025",
                scrapbooks = listOf(
                    Scrapbook("s3a", "1", "Group 1", 9, "brown"),
                    Scrapbook("s3b", "2", "Group 2", 4, "sage")
                )
            ),
            MonthSection(
                month = "February", year = "2025",
                scrapbooks = listOf(
                    Scrapbook("s2a", "1", "Group 1", 7, "brown")
                )
            ),
            MonthSection(
                month = "January", year = "2025",
                scrapbooks = listOf(
                    Scrapbook("s1a", "1", "Group 1", 12, "brown"),
                    Scrapbook("s1b", "2", "Group 2", 5,  "sage"),
                    Scrapbook("s1c", "3", "Group 3", 8,  "brown")
                )
            )
        )
    }
}
