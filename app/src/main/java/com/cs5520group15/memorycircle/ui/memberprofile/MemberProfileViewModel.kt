/**
 * What: ViewModel that holds UI state and business logic for the Member Profile screen.
 * Who:  Used by MemberProfileScreen.
 * When: Created when MemberProfileScreen is first composed; survives config changes.
 */

package com.cs5520group15.memorycircle.ui.memberprofile

import androidx.lifecycle.ViewModel
import com.cs5520group15.memorycircle.data.FirebaseModule
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What: Subscribes to users/{userId} so the read-only MemberProfileScreen
 *       reflects another user's profile edits live, no manual refresh, no
 *       re-entry. Exposes a tri-state UiState so the screen can distinguish
 *       still-loading from confirmed-missing.
 *
 *       Reads the publicly-visible fields only: name, bio, avatarUrl, and
 *       `emailMasked` (e.g. "1***@test.com"). The real address is held by
 *       Firebase Auth and never surfaces to other users.
 * Who: Used by MemberProfileScreen.
 * When: bind() is called from a LaunchedEffect on screen entry; the listener
 *       detaches on onCleared.
 */
class MemberProfileViewModel : ViewModel() {

    /** Snapshot of the fields rendered on MemberProfileScreen. */
    data class MemberInfo(
        val id:          String,
        val name:        String,
        val emailMasked: String,
        val bio:         String,
        val avatarUrl:   String
    )

    /** Loading vs. confirmed-missing vs. loaded. */
    sealed class UiState {
        data object  Loading                       : UiState()
        data object  NotFound                      : UiState()
        data class   Loaded(val info: MemberInfo)  : UiState()
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var boundUserId: String? = null
    private var listener: ListenerRegistration? = null

    fun bind(userId: String) {
        if (userId == boundUserId) return
        boundUserId = userId
        detach()
        _state.value = UiState.Loading

        listener = FirebaseModule.db.collection("users").document(userId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    _state.value = UiState.NotFound
                    return@addSnapshotListener
                }
                if (snap == null || !snap.exists()) {
                    _state.value = UiState.NotFound
                    return@addSnapshotListener
                }
                _state.value = UiState.Loaded(
                    MemberInfo(
                        id          = snap.id,
                        name        = snap.getString("name").orEmpty(),
                        emailMasked = snap.getString("emailMasked").orEmpty(),
                        bio         = snap.getString("bio").orEmpty(),
                        avatarUrl   = snap.getString("avatarUrl").orEmpty()
                    )
                )
            }
    }

    private fun detach() {
        listener?.remove()
        listener = null
    }

    override fun onCleared() {
        super.onCleared()
        detach()
    }
}
