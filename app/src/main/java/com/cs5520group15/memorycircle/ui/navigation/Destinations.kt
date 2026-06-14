package com.cs5520group15.memorycircle.ui.navigation

import kotlinx.serialization.Serializable

/**
 * What: Defines all possible navigation destinations in the app as type-safe route objects.
 * Who: Used by MemoryCircleNavigation and any screen that triggers navigation.
 * When: Referenced every time the NavController navigates to a new screen.
 */

// object = a singleton route with no arguments
// Used for screens that don't need any data passed in
@Serializable object Login
@Serializable object Register
@Serializable object Home
@Serializable object Friends
@Serializable object Profile

// Opens a group's collaborative timeline of memory time points (editable / live).
@Serializable data class ScrapbookViewer(val groupId: String)

// Opens a read-only view of a past month's scrapbook for one group — no Add photo,
// no comment posting, no FAB, no menu. Reached from the Memories calendar and from
// the per-month list on GroupDetail.
@Serializable data class ScrapbookHistory(
    val groupId: String,
    val month:   String,  // e.g. "March"
    val year:    String   // e.g. "2025"
)

// data class = a route that carries arguments
// entryId == null  → create a brand-new time point
// entryId != null  → join (add my photo + description to) an existing time point
@Serializable data class ScrapbookDetail(val groupId: String, val entryId: String? = null)

@Serializable object Memories

// Create-a-new-group flow (pick contacts) — opened from the Home "+" FAB
@Serializable object CreateGroup

// A group's members page — opened from the "View all members" link on GroupDetail
@Serializable data class GroupMembers(val groupId: String)

// A group's detail / settings page — opened from the menu icon on the timeline top bar.
// Holds the group name, the member thumbnail grid (with a "view all" link), the
// per-month scrapbook list for this group, and a "leave group" action in the top bar.
@Serializable data class GroupDetail(val groupId: String)