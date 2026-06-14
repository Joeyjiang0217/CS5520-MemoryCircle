package com.cs5520group15.memorycircle.ui.scrapbookviewer

import androidx.lifecycle.ViewModel
import com.cs5520group15.memorycircle.data.ScrapbookRepository

/**
 * What: Mediates the viewer's edits (title edits, comments) to the shared
 *       ScrapbookRepository. The screen collects the repository's StateFlow
 *       directly for display, so any mutation here propagates automatically.
 *       Edits live in memory for now; Firestore will persist them later.
 * Who: Used by ScrapbookViewerScreen.
 * When: Created when the viewer is displayed; bound to a group on first load.
 */
class ScrapbookViewerViewModel : ViewModel() {

    private var groupId: String? = null

    fun bind(groupId: String) {
        this.groupId = groupId
    }

    fun updateEntryTitle(entryId: String, title: String) {
        groupId?.let { ScrapbookRepository.updateTitle(it, entryId, title) }
    }

    fun addComment(entryId: String, author: String, text: String) {
        groupId?.let { ScrapbookRepository.addComment(it, entryId, author, text) }
    }
}
