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

// Full-screen search overlay opened from the Friends screen's search bar.
// Lets the user fuzzy-match friends (by name or email) and groups (by name).
@Serializable object FriendsSearch

// "See all" friend requests page — opened from the See all link in the Friends
// tab's FRIEND REQUESTS section. Shows every request (pending/accepted/declined),
// supports per-row swipe-to-dismiss.
@Serializable object AllFriendRequests

// "Add new friend" landing page — opened from the person-add icon on the
// Friends tab hero. Holds only a tap-only search bar.
@Serializable object AddFriend

// Active "add friend" search overlay — opened by tapping the search bar on
// AddFriendScreen. Auto-focuses the TextField; results are people only.
@Serializable object AddFriendSearch

// Profile edit form — opened from the "Edit Profile" button on ProfileScreen.
// Holds rows for avatar, name, bio, email; each row is tap-to-edit.
@Serializable object EditProfile

// Full-size avatar viewer — opened from the Avatar row on EditProfile. Top
// bar carries a more-options icon that surfaces pick / save / cancel.
@Serializable object AvatarViewer

// Account settings hub — opened from the Settings row on EditProfile. Holds
// Profile, Notifications, and Log out.
@Serializable object Settings

// Notification toggles — opened from the Notifications row on Settings.
@Serializable object NotificationSettings

// Debug-only Dev Tools page — opened from the Dev Tools row on Settings.
// Hosts seed buttons used to pre-populate Firestore during development.
@Serializable object DevTools

// Read-only profile view for someone OTHER than the current user — opened by
// tapping any friend/member/result/request row across the app. Email is
// privacy-masked on this surface.
@Serializable data class MemberProfile(val userId: String)

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

// Create-a-new-group flow (pick contacts) — opened from the Home "+" FAB.
// Re-used as the "invite new members" surface when reached from GroupDetail's
// invite tile: isInviteMode flips the top-bar title and CTA label, and the
// nav layer routes the confirm action back to the parent group instead of
// minting a new ScrapbookViewer destination.
@Serializable data class CreateGroup(val isInviteMode: Boolean = false)

// A group's members page — opened from the "View all members" link on GroupDetail
@Serializable data class GroupMembers(val groupId: String)

// A group's detail / settings page — opened from the menu icon on the timeline top bar.
// Holds the group name, the member thumbnail grid (with a "view all" link), the
// per-month scrapbook list for this group, and a "leave group" action in the top bar.
@Serializable data class GroupDetail(val groupId: String)