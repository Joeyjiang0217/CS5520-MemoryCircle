package com.cs5520group15.memorycircle.ui.scrapbook

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: Holds the editable state for one scrapbook's timeline — the entries with
 *       their (member-editable) titles/descriptions and member comments.
 *       Edits live in memory for now; Firestore will persist them later.
 * Who: Used by ScrapbookViewerScreen.
 * When: Created when the viewer is displayed; loaded once per scrapbook.
 */
class ScrapbookViewerViewModel : ViewModel() {

    private val _entries = MutableStateFlow<List<ScrapbookEntry>>(emptyList())
    val entries: StateFlow<List<ScrapbookEntry>> = _entries.asStateFlow()

    private var initialized = false

    /**
     * What: Loads the timeline entries once (mock data for now).
     * Who: Called by ScrapbookViewerScreen on first composition.
     * When: Once per ViewModel; later calls are ignored so in-memory edits survive.
     */
    fun loadIfNeeded(groupId: String, memberCount: Int) {
        if (initialized) return
        _entries.value = ScrapbookMockData.getMockEntries(groupId, memberCount)
        initialized = true
    }

    /**
     * What: Updates an entry's title and description (any member can edit).
     *       A blank title falls back to the existing one.
     * Who: Called by ScrapbookViewerScreen when a member saves an edit.
     * When: On tapping "Done" in edit mode.
     */
    fun updateEntryText(entryId: String, title: String, description: String) {
        _entries.value = _entries.value.map { entry ->
            if (entry.id == entryId) {
                entry.copy(
                    title       = title.ifBlank { entry.title },
                    description = description
                )
            } else entry
        }
    }

    /**
     * What: Appends a member's comment to an entry. Blank comments are ignored.
     * Who: Called by ScrapbookViewerScreen when a member posts a comment.
     * When: On tapping "Post".
     */
    fun addComment(entryId: String, author: String, text: String) {
        if (text.isBlank()) return
        _entries.value = _entries.value.map { entry ->
            if (entry.id == entryId) {
                val newComment = Comment(
                    id     = "${entryId}_c${entry.comments.size + 1}",
                    author = author,
                    text   = text.trim()
                )
                entry.copy(comments = entry.comments + newComment)
            } else entry
        }
    }
}
