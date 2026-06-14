package com.cs5520group15.memorycircle.ui.groupmembers

import androidx.lifecycle.ViewModel
import com.cs5520group15.memorycircle.model.Member
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: Holds the roster for one group's members page — the group name and the
 *       flat list of members.
 * Who: Used by GroupMembersScreen.
 * When: Created when the screen is shown for a specific groupId; survives config
 *       changes. Firestore will replace the mock source later.
 */
class GroupMembersViewModel : ViewModel() {

    private val _groupName = MutableStateFlow("")
    private val _members   = MutableStateFlow<List<Member>>(emptyList())

    val groupName: StateFlow<String>       = _groupName.asStateFlow()
    val members:   StateFlow<List<Member>> = _members.asStateFlow()

    fun bind(groupId: String) {
        _groupName.value = mockGroupName(groupId)
        _members.value   = mockMembers(groupId)
    }

    private fun mockGroupName(groupId: String): String = when (groupId) {
        "1"  -> "Weekend Crew"
        "2"  -> "Family Circle"
        "3"  -> "Travel Buddies"
        else -> "Group $groupId"
    }

    private fun mockMembers(groupId: String): List<Member> = when (groupId) {
        "1" -> listOf(
            Member("u_sarah", "Sarah Chen",  34, isOnline = true),
            Member("u_emma",  "Emma Wilson", 28, isOnline = true),
            Member("u_james", "James Liu",   21),
            Member("u_mia",   "Mia Torres",  18),
            Member("u_lila",  "Lila Nguyen",  9)
        )
        "2" -> listOf(
            Member("u_sarah", "Sarah Chen",  42, isOnline = true),
            Member("u_dad",   "David Chen",  30),
            Member("u_mom",   "Helen Chen",  27)
        )
        else -> listOf(
            Member("u_sarah", "Sarah Chen",   16, isOnline = true),
            Member("u_alex",  "Alex Park",    14),
            Member("u_zoe",   "Zoe Martin",   11),
            Member("u_kai",   "Kai Nakamura",  8),
            Member("u_noah",  "Noah Bennett",  6),
            Member("u_riya",  "Riya Patel",    4)
        )
    }
}
