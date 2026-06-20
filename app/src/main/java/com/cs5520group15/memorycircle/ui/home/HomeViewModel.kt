/**
 * What: ViewModel that holds UI state and business logic for the Home screen.
 * Who:  Used by HomeScreen.
 * When: Created when HomeScreen is first composed; survives configuration changes.
 */

package com.cs5520group15.memorycircle.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.AuthRepository
import com.cs5520group15.memorycircle.data.FirebaseModule
import com.cs5520group15.memorycircle.data.ProfileRepository
import com.cs5520group15.memorycircle.data.Result
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What: Holds all UI state for the Home screen.
 *       Subscribes to a Firestore array-contains query on `groups.memberIds`
 *       and re-publishes the matching docs as a list the UI can render.
 * Who: Used by HomeScreen.
 * When: Created when HomeScreen is first displayed, survives configuration changes.
 */
class HomeViewModel : ViewModel() {

    /**
     * What: A single memory group (a circle of people) on the Home screen.
     *       A group is NOT an event — it's a circle that shares memories.
     */
    data class Group(
        val id:          String,
        val name:        String,
        val date:        String,   // subtitle: member summary
        val memoryCount: Int,
        val colorType:   String    // "brown" or "sage" — card gradient
    )

    private val _groups   = MutableStateFlow<List<Group>>(emptyList())
    private val _userName = MutableStateFlow("")

    val groups:   StateFlow<List<Group>> = _groups.asStateFlow()
    val userName: StateFlow<String>      = _userName.asStateFlow()

    /** Live avatar URL from the shared ProfileRepository — empty when the user
     *  has never uploaded a picture. Shared with Profile / EditProfile screens
     *  so an avatar uploaded from those flows shows here without a reload. */
    val profile = ProfileRepository.profile

    /** Active Firestore listener — detached in onCleared() to avoid leaks. */
    private var groupsListener: ListenerRegistration? = null

    init {
        ProfileRepository.bind()
        loadGroups()
        loadUserName()
    }

    /**
     * What: Reads the current user's display name from Firestore via
     *       AuthRepository.getCurrentUserName() and publishes it.
     */
    private fun loadUserName() = viewModelScope.launch {
        when (val result = AuthRepository.getCurrentUserName()) {
            is Result.Success -> _userName.value = result.data
            is Result.Error   -> { /* keep blank on failure */ }
            is Result.Loading -> { }
        }
    }

    /**
     * What: Real-time subscription to every group whose memberIds contains the
     *       current user's uid. Republishes results to the groups StateFlow.
     */
    private fun loadGroups() {
        val uid = AuthRepository.currentUid ?: return
        groupsListener = FirebaseModule.db.collection("groups")
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                _groups.value = snapshot.documents.map { doc ->
                    val memberCount = (doc.getLong("memberCount") ?: 0L).toInt()
                    Group(
                        id          = doc.id,
                        name        = doc.getString("name") ?: "",
                        date        = "$memberCount members",
                        memoryCount = (doc.getLong("memoryCount") ?: 0L).toInt(),
                        colorType   = doc.getString("colorType") ?: "brown"
                    )
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        groupsListener?.remove()
        groupsListener = null
    }
}
