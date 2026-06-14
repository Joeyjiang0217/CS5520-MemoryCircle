package com.cs5520group15.memorycircle.ui.memberprofile

import androidx.lifecycle.ViewModel
import com.cs5520group15.memorycircle.data.FriendsRepository
import com.cs5520group15.memorycircle.model.Friend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: Looks up a single user in the shared FriendsRepository pool by userId
 *       and exposes them to MemberProfileScreen. The pool covers both friends
 *       and discoverable strangers, so this VM works whether the caller came
 *       from the friend list, group members, search results, or a friend
 *       request row. Returns null if the id isn't in the pool (e.g., if the
 *       user tried to view their own profile via a group-members row — the
 *       current user isn't seeded into discoverableUsers).
 * Who: Used by MemberProfileScreen.
 * When: bind() is called from a LaunchedEffect on screen entry.
 */
class MemberProfileViewModel : ViewModel() {

    private val _member = MutableStateFlow<Friend?>(null)
    val member: StateFlow<Friend?> = _member.asStateFlow()

    fun bind(userId: String) {
        _member.value = FriendsRepository.discoverableUsers.value.firstOrNull { it.id == userId }
    }
}
