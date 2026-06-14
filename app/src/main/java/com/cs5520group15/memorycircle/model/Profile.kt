package com.cs5520group15.memorycircle.model

/**
 * What: The current user's profile fields surfaced on the Profile tab and
 *       editable on the EditProfile screen.
 * Who: Used by ProfileRepository, ProfileViewModel, EditProfileViewModel.
 * When: Instantiated when the repo is constructed; updated through the repo's
 *       per-field updaters whenever the user saves a row on EditProfile.
 */
data class Profile(
    val name:  String,
    val bio:   String,
    val email: String
)
