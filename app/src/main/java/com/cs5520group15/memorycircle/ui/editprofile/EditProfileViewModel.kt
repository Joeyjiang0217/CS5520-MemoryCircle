/**
 * What: ViewModel that holds UI state and business logic for the Edit Profile screen.
 * Who:  Used by EditProfileScreen.
 * When: Created when EditProfileScreen is first composed; survives config changes.
 */

package com.cs5520group15.memorycircle.ui.editprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.ProfileRepository
import kotlinx.coroutines.launch

/**
 * What: Thin wrapper for EditProfileScreen — exposes the live profile flow
 *       for read, and forwards per-field saves to ProfileRepository which
 *       writes them to Firestore. The shared listener pushes the new value
 *       back through the StateFlow, so ProfileScreen reflects edits on
 *       back-navigation without any explicit refresh.
 * Who: Used by EditProfileScreen.
 * When: Created on first composition; survives config changes.
 */
class EditProfileViewModel : ViewModel() {

    init { ProfileRepository.bind() }

    val profile = ProfileRepository.profile

    fun updateName(name: String) = viewModelScope.launch {
        runCatching { ProfileRepository.updateName(name) }
    }

    fun updateBio(bio: String) = viewModelScope.launch {
        runCatching { ProfileRepository.updateBio(bio) }
    }

    fun updateEmail(email: String) = viewModelScope.launch {
        runCatching { ProfileRepository.updateEmail(email) }
    }
}
