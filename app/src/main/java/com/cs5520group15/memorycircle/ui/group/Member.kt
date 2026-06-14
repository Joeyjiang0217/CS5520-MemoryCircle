package com.cs5520group15.memorycircle.ui.group

/**
 * What: One person who belongs to a memory group. Drives the row shown on the
 *       group-members screen (avatar initial + name + shared-memories count).
 *       Kept flat for now — no roles/admin concept until the project actually
 *       needs admin-only actions like "remove member" or transferring ownership.
 * Who: Used by GroupMembersViewModel and GroupMembersScreen.
 * When: Instantiated when loading a group's roster; Firestore will replace the
 *       mock source in a later phase.
 */
data class Member(
    val id:             String,
    val name:           String,
    val sharedMemories: Int,
    val isOnline:       Boolean = false
)
