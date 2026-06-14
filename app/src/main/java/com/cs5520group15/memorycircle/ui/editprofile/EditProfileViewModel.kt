package com.cs5520group15.memorycircle.ui.editprofile

import androidx.lifecycle.ViewModel
import com.cs5520group15.memorycircle.data.ProfileRepository

/**
 * What: Thin wrapper for EditProfileScreen — exposes the live profile flow
 *       for read, and forwards per-field saves to the shared repository so
 *       ProfileScreen reflects them immediately on back-navigation.
 * Who: Used by EditProfileScreen.
 * When: Created on first composition; survives config changes.
 */
class EditProfileViewModel : ViewModel() {

    val profile = ProfileRepository.profile

    fun updateName(name: String)   = ProfileRepository.updateName(name)
    fun updateBio(bio: String)     = ProfileRepository.updateBio(bio)
    fun updateEmail(email: String) = ProfileRepository.updateEmail(email)
}
