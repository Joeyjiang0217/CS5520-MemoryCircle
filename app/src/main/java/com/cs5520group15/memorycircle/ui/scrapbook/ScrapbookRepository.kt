package com.cs5520group15.memorycircle.ui.scrapbook

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: A stand-in for the logged-in user until real auth/DB exists. Used as the
 *       member name on contributions and the author on comments.
 * Who: Used by the scrapbook screens.
 * When: Whenever the current user adds a photo/description or posts a comment.
 */
object CurrentUser {
    const val name = "Sarah"
}

/**
 * What: In-memory store for every group's scrapbook timeline. Both the viewer and
 *       the creation screen read and write through this single source of truth, so
 *       a time point created on one screen shows up on the other (and survives
 *       navigation). Seeded from ScrapbookMockData on first access; Firestore will
 *       replace it later.
 * Who: Used by ScrapbookViewerViewModel and ScrapbookViewModel.
 * When: Accessed whenever a timeline is shown, created, or edited.
 */
object ScrapbookRepository {

    // groupId -> that group's timeline entries.
    private val flows = mutableMapOf<String, MutableStateFlow<List<ScrapbookEntry>>>()

    /**
     * What: Returns the (seeded) timeline flow for a group, creating it on first use.
     * Who: Called by the ViewModels to observe and read entries.
     * When: On screen load and before any mutation.
     */
    fun entriesFor(groupId: String): StateFlow<List<ScrapbookEntry>> = flow(groupId).asStateFlow()

    private fun flow(groupId: String): MutableStateFlow<List<ScrapbookEntry>> =
        flows.getOrPut(groupId) {
            MutableStateFlow(ScrapbookMockData.getMockEntries(groupId))
        }

    /**
     * What: Creates a brand-new time point. The creator sets title + tags and adds
     *       the first contribution. Entries stay sorted by day within the month.
     * Who: Called by ScrapbookViewModel when the "+" (new time point) flow saves.
     * When: On save in new-entry mode.
     */
    fun addEntry(
        groupId:           String,
        date:              String,
        title:             String,
        tags:              List<String>,
        firstContribution: MemberContribution
    ) {
        val f = flow(groupId)
        val newEntry = ScrapbookEntry(
            id            = "e${System.currentTimeMillis()}",
            date          = date,
            title         = title,
            tags          = tags,
            contributions = listOf(firstContribution)
        )
        f.value = (f.value + newEntry).sortedBy { dayOf(it.date) }
    }

    /**
     * What: Appends a member's contribution (photo + description) to an existing
     *       time point — the "join an existing card" flow.
     * Who: Called by ScrapbookViewModel when the join flow saves.
     * When: On save in join mode.
     */
    fun addContribution(groupId: String, entryId: String, contribution: MemberContribution) {
        val f = flow(groupId)
        f.value = f.value.map { entry ->
            if (entry.id == entryId) entry.copy(contributions = entry.contributions + contribution)
            else entry
        }
    }

    /**
     * What: Looks up a single entry (used by the join flow to pre-fill title/tags).
     * Who: Called by ScrapbookViewModel.loadForJoin.
     * When: When opening the creation screen to join an existing time point.
     */
    fun entry(groupId: String, entryId: String): ScrapbookEntry? =
        flow(groupId).value.firstOrNull { it.id == entryId }

    /**
     * What: Updates an entry's title (any member can edit). Blank falls back to the
     *       existing title.
     * Who: Called by ScrapbookViewerViewModel on inline title edit.
     * When: On tapping "Done" in edit mode.
     */
    fun updateTitle(groupId: String, entryId: String, title: String) {
        val f = flow(groupId)
        f.value = f.value.map { entry ->
            if (entry.id == entryId) entry.copy(title = title.ifBlank { entry.title }) else entry
        }
    }

    /**
     * What: Appends a member's comment to an entry. Blank comments are ignored.
     * Who: Called by ScrapbookViewerViewModel when a member posts a comment.
     * When: On tapping "Post".
     */
    fun addComment(groupId: String, entryId: String, author: String, text: String) {
        if (text.isBlank()) return
        val f = flow(groupId)
        f.value = f.value.map { entry ->
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

    // "June 10" -> 10, for keeping entries in day order.
    private fun dayOf(date: String): Int =
        date.substringAfterLast(' ').trim().toIntOrNull() ?: 0
}
