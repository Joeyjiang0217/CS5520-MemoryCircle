package com.cs5520group15.memorycircle.ui.scrapbookviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.ScrapbookRepository
import kotlinx.coroutines.launch

/**
 * What: Mediates the viewer's edits (title, photo description, comments) to
 *       the shared ScrapbookRepository. The screen collects the repository's
 *       StateFlow directly for display, so any mutation here propagates
 *       automatically once Firestore confirms it.
 * Who: Used by ScrapbookViewerScreen.
 * When: Created when the viewer is displayed; bound to a group on first load.
 */
class ScrapbookViewerViewModel : ViewModel() {

    private var groupId: String? = null

    /** Binds this ViewModel to the group whose timeline is being viewed. */
    fun bind(groupId: String) {
        this.groupId = groupId
    }

    fun updateEntryTitle(entryId: String, title: String) {
        val gid = groupId ?: return
        viewModelScope.launch {
            try {
                ScrapbookRepository.updateTitle(gid, entryId, title)
            } catch (_: Exception) { /* keep UI as-is on failure */ }
        }
    }

    /**
     * Updates the description of a single photo on a post. The `author` arg
     * is unused (kept for caller compatibility) — the repository edits the
     * photo by photoId regardless of who's editing.
     */
    fun updateDescription(entryId: String, photoId: String, description: String) {
        val gid = groupId ?: return
        viewModelScope.launch {
            try {
                ScrapbookRepository.updatePhotoDescription(gid, entryId, photoId, description)
            } catch (_: Exception) { }
        }
    }

    /**
     * Appends a comment. Author is resolved to the real current user inside
     * the repository; the legacy `author` parameter is ignored.
     */
    fun addComment(entryId: String, author: String, text: String) {
        val gid = groupId ?: return
        viewModelScope.launch {
            try {
                ScrapbookRepository.addComment(gid, entryId, text)
            } catch (_: Exception) { }
        }
    }

    override fun onCleared() {
        super.onCleared()
        groupId?.let { ScrapbookRepository.detach(it) }
    }
}
