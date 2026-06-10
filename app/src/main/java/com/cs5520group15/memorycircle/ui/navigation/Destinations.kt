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

@Serializable data class ScrapbookViewer(val groupId: String)

// data class = a route that carries arguments
// Used for screens that need data to know what to display
@Serializable data class ScrapbookDetail(val groupId: String)

@Serializable object Memories

// Create-a-new-group flow (pick contacts) — opened from the Home "+" FAB
@Serializable object CreateGroup