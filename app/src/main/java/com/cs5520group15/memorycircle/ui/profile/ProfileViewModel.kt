/**
 * What: ViewModel that holds UI state and business logic for the Profile screen.
 * Who:  Used by ProfileScreen.
 * When: Created when ProfileScreen is first composed; survives config changes.
 */

package com.cs5520group15.memorycircle.ui.profile

import androidx.lifecycle.ViewModel
import com.cs5520group15.memorycircle.data.ProfileRepository

/**
 * What: Read-only wrapper exposing the current profile to ProfileScreen and
 *       AvatarViewerScreen. The landing-tab screen never mutates the profile;
 *       all edits are routed through EditProfileViewModel.
 * Who: Used by ProfileScreen and AvatarViewerScreen.
 * When: Created on first composition; survives config changes. The init block
 *       triggers ProfileRepository.bind() so the Firestore listener attaches
 *       even if the user lands directly on ProfileScreen without going through
 *       EditProfile first.
 */
class ProfileViewModel : ViewModel() {

    init { ProfileRepository.bind() }

    val profile = ProfileRepository.profile
}
