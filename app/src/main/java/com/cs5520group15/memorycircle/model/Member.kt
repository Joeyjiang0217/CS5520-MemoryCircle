/**
 * What: Data model for one person who belongs to a memory group, driving the
 *       group-members row (avatar + name + shared-memories count).
 * Who:  Used by GroupDetailViewModel / GroupMembersViewModel and their screens.
 * When: Instantiated whenever a group's roster is rebuilt from memberIds plus
 *       AuthRepository.getUserBriefs.
 */

package com.cs5520group15.memorycircle.model

/**
 * What: One person who belongs to a memory group. Drives the row shown on the
 *       group-members screen (avatar + name + shared-memories count).
 *       Kept flat — no roles/admin concept until the project actually needs
 *       admin-only actions like "remove member" or transferring ownership.
 *       avatarUrl is the Firebase Storage download URL for the user's profile
 *       picture; blank means the row should fall back to the letter avatar.
 * Who: Used by GroupDetailViewModel / GroupMembersViewModel and their screens.
 * When: Instantiated whenever a group's roster is rebuilt from memberIds +
 *       AuthRepository.getUserBriefs.
 */
data class Member(
    val id:             String,
    val name:           String,
    val sharedMemories: Int,
    val isOnline:       Boolean = false,
    val avatarUrl:      String  = "",
    val bio:            String  = ""
)
