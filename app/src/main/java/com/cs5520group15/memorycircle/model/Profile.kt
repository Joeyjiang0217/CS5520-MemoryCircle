package com.cs5520group15.memorycircle.model

/**
 * What: The current user's profile fields surfaced on the Profile tab and
 *       editable on the EditProfile screen. avatarUrl is the Firebase Storage
 *       download URL of the user's profile picture, or "" when they haven't
 *       uploaded one yet (in which case the UI falls back to a letter avatar).
 * Who: Used by ProfileRepository, ProfileViewModel, EditProfileViewModel,
 *      AvatarViewerScreen.
 * When: Republished by ProfileRepository whenever the users/{uid} doc changes.
 */
data class Profile(
    val name:      String,
    val bio:       String,
    val email:     String,
    val avatarUrl: String = ""
)
