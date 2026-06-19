package com.cs5520group15.memorycircle.ui.groupmembers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.AuthRepository
import com.cs5520group15.memorycircle.data.FirebaseModule
import com.cs5520group15.memorycircle.model.Member
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What: Holds the roster for one group's members page. Both the group name AND
 *       the member list come from the same Firestore source — the group main
 *       document — so a single listener powers both StateFlows:
 *         - groupName: groups/{gid}.name
 *         - members:   derived from groups/{gid}.memberIds (uids resolved to
 *                      display names via AuthRepository.getUserNames).
 *       There is no separate `members` subcollection — `memberIds` IS the
 *       membership source of truth, same convention as GroupDetailViewModel.
 * Who: Used by GroupMembersScreen.
 * When: Created when the screen is shown for a specific groupId; survives config
 *       changes. Listener detached in onCleared().
 */
class GroupMembersViewModel : ViewModel() {

    private val _groupName = MutableStateFlow("")
    private val _members   = MutableStateFlow<List<Member>>(emptyList())

    val groupName: StateFlow<String>       = _groupName.asStateFlow()
    val members:   StateFlow<List<Member>> = _members.asStateFlow()

    private var boundGroupId: String? = null
    private var groupListener: ListenerRegistration? = null

    fun bind(groupId: String) {
        if (boundGroupId == groupId) return
        boundGroupId = groupId
        detachAll()

        val groupRef = FirebaseModule.db.collection("groups").document(groupId)

        groupListener = groupRef.addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            _groupName.value = snap.getString("name") ?: "Untitled"

            @Suppress("UNCHECKED_CAST")
            val uids = (snap.get("memberIds") as? List<String>) ?: emptyList()
            viewModelScope.launch {
                val briefs = AuthRepository.getUserBriefs(uids)
                _members.value = uids.map { uid ->
                    val brief = briefs[uid]
                    Member(
                        id             = uid,
                        name           = brief?.name ?: "Member",
                        sharedMemories = 0,
                        isOnline       = false,
                        avatarUrl      = brief?.avatarUrl.orEmpty()
                    )
                }
            }
        }
    }

    private fun detachAll() {
        groupListener?.remove(); groupListener = null
    }

    override fun onCleared() {
        super.onCleared()
        detachAll()
    }
}
