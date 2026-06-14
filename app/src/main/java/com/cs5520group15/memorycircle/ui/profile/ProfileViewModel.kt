package com.cs5520group15.memorycircle.ui.profile

import androidx.lifecycle.ViewModel

/**
 * What: Read-only wrapper exposing the current profile to ProfileScreen and
 *       AvatarViewerScreen. The landing-tab screen never mutates the profile;
 *       all edits are routed through EditProfileViewModel.
 * Who: Used by ProfileScreen and AvatarViewerScreen.
 * When: Created on first composition; survives config changes.
 */
class ProfileViewModel : ViewModel() {
    val profile = ProfileRepository.profile
}
