package com.cs5520group15.memorycircle.model

/**
 * What: The three notification toggles surfaced on NotificationSettingsScreen.
 *       Each flag controls one push channel:
 *         - newFriendRequests: fired when someone sends the user a friend
 *           invitation (Friends tab "FRIEND REQUESTS" surfaces the entry).
 *         - newGroupActivity: fired when the user is added to a new group,
 *           or when a group they belong to has a meaningful change (members
 *           joined, group renamed, etc.).
 *         - newMemoryPosts: fired when a group member adds a new
 *           memory post (a photo + caption) to a scrapbook. A scrapbook
 *           collects a month's posts, so the user gets a ping per post,
 *           not per month.
 * Who: Used by NotificationSettingsRepository and NotificationSettingsScreen.
 * When: Default all-on; the user toggles individually on the settings page.
 */
data class NotificationSettings(
    val newFriendRequests: Boolean = true,
    val newGroupActivity:  Boolean = true,
    val newMemoryPosts:    Boolean = true
)
