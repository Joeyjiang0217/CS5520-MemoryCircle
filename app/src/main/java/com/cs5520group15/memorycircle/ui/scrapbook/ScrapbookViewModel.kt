package com.cs5520group15.memorycircle.ui.scrapbook

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: Holds all UI state for the Scrapbook creation and viewer screens.
 * Who: Used by ScrapbookScreen and ScrapbookViewerScreen.
 * When: Created when either scrapbook screen is displayed.
 */
class ScrapbookViewModel : ViewModel() {

    // --- Data Models ---

    /**
     * What: Represents a single photo item in a scrapbook page.
     * Who: Used by ScrapbookViewModel and ScrapbookViewerScreen.
     * When: Instantiated when loading photos for the scrapbook.
     */
    data class PhotoItem(
        val id:       String,
        val url:      String,
        val date:     String,
        val caption:  String = ""
    )

    /**
     * What: Represents one page in the scrapbook with a layout template.
     * Who: Used by ScrapbookViewerScreen to render each page.
     * When: Generated when the user taps "Generate Scrapbook".
     */
    data class ScrapbookPage(
        val id:       String,
        val photos:   List<PhotoItem>,
        val template: String,  // "grid2", "grid4", "grid6"
        val date:     String
    )

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

    // --- State ---
    private val _selectedTemplate = MutableStateFlow("grid4")
    private val _selectedGroupId  = MutableStateFlow<String?>(null)
    private val _journalEntry     = MutableStateFlow("")
    private val _tags             = MutableStateFlow(listOf("#2026", "#friends"))
    private val _pages            = MutableStateFlow<List<ScrapbookPage>>(emptyList())
    private val _isGenerated      = MutableStateFlow(false)

    val selectedTemplate: StateFlow<String>           = _selectedTemplate.asStateFlow()
    val selectedGroupId:  StateFlow<String?>          = _selectedGroupId.asStateFlow()
    val journalEntry:     StateFlow<String>           = _journalEntry.asStateFlow()
    val tags:             StateFlow<List<String>>     = _tags.asStateFlow()
    val pages:            StateFlow<List<ScrapbookPage>> = _pages.asStateFlow()
    val isGenerated:      StateFlow<Boolean>          = _isGenerated.asStateFlow()

    // Dummy photos for testing — Firebase will replace this later
    private val dummyPhotos = listOf(
        PhotoItem("1", "https://picsum.photos/seed/a/400/400", "June 1, 2025"),
        PhotoItem("2", "https://picsum.photos/seed/b/400/400", "June 1, 2025"),
        PhotoItem("3", "https://picsum.photos/seed/c/400/400", "June 2, 2025"),
        PhotoItem("4", "https://picsum.photos/seed/d/400/400", "June 2, 2025"),
        PhotoItem("5", "https://picsum.photos/seed/e/400/400", "June 3, 2025"),
        PhotoItem("6", "https://picsum.photos/seed/f/400/400", "June 3, 2025"),
        PhotoItem("7", "https://picsum.photos/seed/g/400/400", "June 4, 2025"),
        PhotoItem("8", "https://picsum.photos/seed/h/400/400", "June 4, 2025")
    )

    /**
     * What: Updates the selected layout template.
     * Who: Called by ScrapbookScreen when user taps a template preview.
     * When: On template selection.
     */
    fun onTemplateSelected(template: String) {
        _selectedTemplate.value = template
    }

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

    /**
     * What: Generates scrapbook pages from dummy photos using the selected template.
     *       Groups photos into pages based on template size (2, 4, or 6 per page).
     * Who: Called by ScrapbookScreen when user taps "Generate Scrapbook".
     * When: On Generate button click.
     */
    fun onGenerate() {
        val photosPerPage = when (_selectedTemplate.value) {
            "grid2" -> 2
            "grid6" -> 6
            else    -> 4  // grid4 default
        }

        // Split dummy photos into pages
        val generatedPages = dummyPhotos
            .chunked(photosPerPage)
            .mapIndexed { index, photos ->
                ScrapbookPage(
                    id       = "page_$index",
                    photos   = photos,
                    template = _selectedTemplate.value,
                    date     = photos.firstOrNull()?.date ?: ""
                )
            }

        _pages.value     = generatedPages
        _isGenerated.value = true
    }
}