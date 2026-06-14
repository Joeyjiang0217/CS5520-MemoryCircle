package com.cs5520group15.memorycircle.data

import com.cs5520group15.memorycircle.model.NotificationSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: Single source of truth for the user's notification preferences.
 *       Holds an in-memory NotificationSettings; per-field setters mutate the
 *       flow so the toggles update instantly.
 *       Survives navigation but resets on app restart — DataStore later.
 * Who: Used by NotificationSettingsScreen.
 * When: First touched when the user opens Settings → Notifications.
 */
object NotificationSettingsRepository {

    private val _settings = MutableStateFlow(NotificationSettings())
    val settings: StateFlow<NotificationSettings> = _settings.asStateFlow()

    fun setNewFriendRequests(enabled: Boolean) {
        _settings.value = _settings.value.copy(newFriendRequests = enabled)
    }

    fun setNewGroupActivity(enabled: Boolean) {
        _settings.value = _settings.value.copy(newGroupActivity = enabled)
    }

    fun setNewMemoryPosts(enabled: Boolean) {
        _settings.value = _settings.value.copy(newMemoryPosts = enabled)
    }
}
