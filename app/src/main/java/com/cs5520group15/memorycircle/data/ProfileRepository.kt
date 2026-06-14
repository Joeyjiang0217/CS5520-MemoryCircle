package com.cs5520group15.memorycircle.data

import com.cs5520group15.memorycircle.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: In-memory store for the current user's profile. Both ProfileScreen
 *       (read-only) and EditProfileScreen (read + write) point here, so a
 *       row edited on one screen is immediately reflected on the other.
 *       Survives navigation, resets on app restart — Firestore later.
 * Who: Used by ProfileViewModel and EditProfileViewModel.
 * When: First touched when the Profile tab is opened.
 */
object ProfileRepository {

    private val _profile = MutableStateFlow(
        Profile(
            name  = "Sarah Chen",
            bio   = "Collecting moments, one memory at a time.",
            email = "sarah.chen@gmail.com"
        )
    )
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    fun updateName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _profile.value = _profile.value.copy(name = trimmed)
    }

    fun updateBio(bio: String) {
        // Blank bio is allowed — the user might intentionally clear it.
        _profile.value = _profile.value.copy(bio = bio.trim())
    }

    fun updateEmail(email: String) {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return
        _profile.value = _profile.value.copy(email = trimmed)
    }
}
