/**
 * What: ViewModel that holds UI state and business logic for the Group Members screen.
 * Who:  Used by GroupMembersScreen.
 * When: Created when GroupMembersScreen is first composed; survives config changes.
 */

package com.cs5520group15.memorycircle.ui.groupmembers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.FirebaseModule
import com.cs5520group15.memorycircle.data.GroupRepository
import com.cs5520group15.memorycircle.model.Member
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What: Holds the roster for one group's members page.
 *
 *       Two live Firestore subscriptions:
 *         - groups/{gid}            → name only.
 *         - groups/{gid}/members    → denormalized {name, avatarUrl} per
 *                                     member. Cross-device profile edits
 *                                     propagate via the fan-out
 *                                     ProfileRepository performs on save.
 *
 * Who: Used by GroupMembersScreen.
 * When: Created when the screen is shown for a specific groupId; survives
 *       config changes. Listeners detached in onCleared().
 */
class GroupMembersViewModel : ViewModel() {

    private val _groupName = MutableStateFlow("")
    private val _members   = MutableStateFlow<List<Member>>(emptyList())

    val groupName: StateFlow<String>       = _groupName.asStateFlow()
    val members:   StateFlow<List<Member>> = _members.asStateFlow()

    private var boundGroupId:    String? = null
    private var groupListener:   ListenerRegistration? = null
    private var membersListener: ListenerRegistration? = null

    fun bind(groupId: String) {
        if (boundGroupId == groupId) return
        boundGroupId = groupId
        detachAll()

        // Back-fill missing members/{uid} subdocs for legacy groups — same
        // pattern as GroupDetailViewModel.bind. Idempotent.
        viewModelScope.launch {
            runCatching { GroupRepository.reconcileMembers(groupId) }
        }

        val groupRef = FirebaseModule.db.collection("groups").document(groupId)

        groupListener = groupRef.addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            _groupName.value = snap.getString("name") ?: "Untitled"
        }

        membersListener = groupRef.collection("members")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _members.value = snap.documents
                    .map { doc ->
                        Member(
                            id             = doc.id,
                            name           = doc.getString("name").orEmpty(),
                            sharedMemories = 0,
                            isOnline       = false,
                            avatarUrl      = doc.getString("avatarUrl").orEmpty(),
                            bio            = doc.getString("bio").orEmpty()
                        )
                    }
                    .sortedBy { it.name.lowercase() }
            }
    }

    private fun detachAll() {
        groupListener?.remove();   groupListener   = null
        membersListener?.remove(); membersListener = null
    }

    override fun onCleared() {
        super.onCleared()
        detachAll()
    }
}
