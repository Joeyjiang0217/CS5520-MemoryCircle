package com.cs5520group15.memorycircle.ui.scrapbook

import androidx.lifecycle.ViewModel

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

    /** Binds this ViewModel to the group whose timeline is being viewed. */
    fun bind(groupId: String) {
        this.groupId = groupId
    }

    /**
     * What: Updates an entry's title (any member can edit).
     * Who: Called by ScrapbookViewerScreen when a member saves a title edit.
     * When: On tapping "Done" in edit mode.
     */
    fun updateEntryTitle(entryId: String, title: String) {
        groupId?.let { ScrapbookRepository.updateTitle(it, entryId, title) }
    }

    /**
     * What: Appends a member's comment to an entry. Blank comments are ignored.
     * Who: Called by ScrapbookViewerScreen when a member posts a comment.
     * When: On tapping "Post".
     */
    fun addComment(entryId: String, author: String, text: String) {
        groupId?.let { ScrapbookRepository.addComment(it, entryId, author, text) }
    }
}
