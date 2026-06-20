/**
 * What: ViewModel that holds UI state and business logic for the Dev Tools screen.
 * Who:  Used by DevToolsScreen.
 * When: Created when DevToolsScreen is first composed; survives configuration changes.
 */

package com.cs5520group15.memorycircle.ui.devtools

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.SeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What: Backs the Dev Tools page. Drives the three idempotent seed actions
 *       and tracks per-action state (idle / running / success message / error)
 *       so the UI can show a spinner and a result chip without losing state
 *       on recomposition.
 * Who: Used by DevToolsScreen.
 */
class DevToolsViewModel : ViewModel() {

    /** Per-action lifecycle state shown next to each seed button. */
    sealed class ActionState {
        data object Idle                              : ActionState()
        data object Running                           : ActionState()
        data class  Success(val message: String)      : ActionState()
        data class  Error(val message: String)        : ActionState()
    }

    private val _usersState       = MutableStateFlow<ActionState>(ActionState.Idle)
    private val _postState        = MutableStateFlow<ActionState>(ActionState.Idle)
    private val _historyState     = MutableStateFlow<ActionState>(ActionState.Idle)
    private val _friendsState     = MutableStateFlow<ActionState>(ActionState.Idle)
    private val _profilesState    = MutableStateFlow<ActionState>(ActionState.Idle)
    private val _clearProfilesState = MutableStateFlow<ActionState>(ActionState.Idle)
    private val _acceptForU6State   = MutableStateFlow<ActionState>(ActionState.Idle)

    // Notification simulations
    private val _simFriendReqState     = MutableStateFlow<ActionState>(ActionState.Idle)
    private val _simGroupInviteState   = MutableStateFlow<ActionState>(ActionState.Idle)
    private val _simJoinMyGroupState   = MutableStateFlow<ActionState>(ActionState.Idle)
    private val _simNewPostState       = MutableStateFlow<ActionState>(ActionState.Idle)
    private val _simNewPhotoState      = MutableStateFlow<ActionState>(ActionState.Idle)
    private val _simCommentState       = MutableStateFlow<ActionState>(ActionState.Idle)

    val usersState:         StateFlow<ActionState> = _usersState.asStateFlow()
    val postState:          StateFlow<ActionState> = _postState.asStateFlow()
    val historyState:       StateFlow<ActionState> = _historyState.asStateFlow()
    val friendsState:       StateFlow<ActionState> = _friendsState.asStateFlow()
    val profilesState:      StateFlow<ActionState> = _profilesState.asStateFlow()
    val clearProfilesState: StateFlow<ActionState> = _clearProfilesState.asStateFlow()
    val acceptForU6State:   StateFlow<ActionState> = _acceptForU6State.asStateFlow()

    val simFriendReqState:   StateFlow<ActionState> = _simFriendReqState.asStateFlow()
    val simGroupInviteState: StateFlow<ActionState> = _simGroupInviteState.asStateFlow()
    val simJoinMyGroupState: StateFlow<ActionState> = _simJoinMyGroupState.asStateFlow()
    val simNewPostState:     StateFlow<ActionState> = _simNewPostState.asStateFlow()
    val simNewPhotoState:    StateFlow<ActionState> = _simNewPhotoState.asStateFlow()
    val simCommentState:     StateFlow<ActionState> = _simCommentState.asStateFlow()

    fun seedUsers(appContext: Context) = viewModelScope.launch {
        _usersState.value = ActionState.Running
        _usersState.value = runCatching { SeedRepository.seedTestUsers(appContext) }
            .fold(
                onSuccess = { r ->
                    val tail = buildString {
                        if (r.backfilledUsers > 0) append(" · backfilled ${r.backfilledUsers}")
                        if (r.errors.isNotEmpty()) append(" · ${r.errors.size} errors")
                    }
                    ActionState.Success("Created ${r.createdUsers}, skipped ${r.skippedUsers}$tail")
                },
                onFailure = { ActionState.Error(it.message ?: "Unknown error") }
            )
    }

    fun seedPost() = viewModelScope.launch {
        _postState.value = ActionState.Running
        _postState.value = runCatching { SeedRepository.seedTestPost() }
            .fold(
                onSuccess = { id -> ActionState.Success("Posted (id=${id.take(6)}…) in your first group") },
                onFailure = { ActionState.Error(it.message ?: "Unknown error") }
            )
    }

    fun seedHistory() = viewModelScope.launch {
        _historyState.value = ActionState.Running
        _historyState.value = runCatching { SeedRepository.seedHistoricalScrapbooks() }
            .fold(
                onSuccess = { n ->
                    if (n == 0) ActionState.Success("All three past months already had scrapbooks — nothing to add")
                    else ActionState.Success("Created $n historical scrapbook(s)")
                },
                onFailure = { ActionState.Error(it.message ?: "Unknown error") }
            )
    }

    fun seedFriendships() = viewModelScope.launch {
        _friendsState.value = ActionState.Running
        _friendsState.value = runCatching { SeedRepository.seedFriendships() }
            .fold(
                onSuccess = { n ->
                    if (n == 0) ActionState.Success("Already friends with 1-5@test.com — nothing to add")
                    else ActionState.Success("Befriended $n test user(s) 1-5")
                },
                onFailure = { ActionState.Error(it.message ?: "Unknown error") }
            )
    }

    fun seedProfiles() = viewModelScope.launch {
        _profilesState.value = ActionState.Running
        _profilesState.value = runCatching { SeedRepository.seedTestUserProfiles() }
            .fold(
                onSuccess = { n -> ActionState.Success("Patched bio + avatar on $n test user(s)") },
                onFailure = { ActionState.Error(it.message ?: "Unknown error") }
            )
    }

    fun clearProfiles() = viewModelScope.launch {
        _clearProfilesState.value = ActionState.Running
        _clearProfilesState.value = runCatching { SeedRepository.clearTestUserProfiles() }
            .fold(
                onSuccess = { n -> ActionState.Success("Cleared bio + avatar on $n test user(s)") },
                onFailure = { ActionState.Error(it.message ?: "Unknown error") }
            )
    }

    /**
     * Impersonates Test User 6 and accepts every pending friend request in
     * their inbox. Lets the developer verify the request → accept → friendship
     * flow without signing in as the test user.
     */
    fun acceptForU6() = viewModelScope.launch {
        _acceptForU6State.value = ActionState.Running
        _acceptForU6State.value = runCatching { SeedRepository.acceptIncomingRequestsForTestUser(6) }
            .fold(
                onSuccess = { n ->
                    if (n == 0) ActionState.Success("No pending requests for Test User 6")
                    else ActionState.Success("Accepted $n request(s) for Test User 6")
                },
                onFailure = { ActionState.Error(it.message ?: "Unknown error") }
            )
    }

    // ── Notification simulations ─────────────────────────────────────────────
    // Each writes the same Firestore data a real user action would; the live
    // NotificationsRepository listeners pick it up and fire system
    // notifications. Verifies the full path end-to-end, not just the trigger.

    fun simFriendRequestFromU8() = viewModelScope.launch {
        _simFriendReqState.value = ActionState.Running
        _simFriendReqState.value = runCatching { SeedRepository.simulateFriendRequestFromTestUser(8) }
            .fold(
                onSuccess = { ActionState.Success("Test User 8 sent you a friend request") },
                onFailure = { ActionState.Error(it.message ?: "Unknown error") }
            )
    }

    fun simGroupInviteFromU8() = viewModelScope.launch {
        _simGroupInviteState.value = ActionState.Running
        _simGroupInviteState.value = runCatching { SeedRepository.simulateGroupInviteFromTestUser(8) }
            .fold(
                onSuccess = { ActionState.Success("Test User 8's sim group ready — you're a member") },
                onFailure = { ActionState.Error(it.message ?: "Unknown error") }
            )
    }

    fun simU10JoinMyGroup() = viewModelScope.launch {
        _simJoinMyGroupState.value = ActionState.Running
        _simJoinMyGroupState.value = runCatching { SeedRepository.simulateUserJoiningMyGroup(10) }
            .fold(
                onSuccess = { ActionState.Success("Test User 10 joined your group") },
                onFailure = { ActionState.Error(it.message ?: "Unknown error") }
            )
    }

    fun simNewPostByU8() = viewModelScope.launch {
        _simNewPostState.value = ActionState.Running
        _simNewPostState.value = runCatching { SeedRepository.simulateNewPostByTestUser(8) }
            .fold(
                onSuccess = { ActionState.Success("Test User 8 posted in their sim group") },
                onFailure = { ActionState.Error(it.message ?: "Unknown error") }
            )
    }

    fun simNewPhotoByU8() = viewModelScope.launch {
        _simNewPhotoState.value = ActionState.Running
        _simNewPhotoState.value = runCatching { SeedRepository.simulateNewPhotoByTestUser(8) }
            .fold(
                onSuccess = { ActionState.Success("Test User 8 added a photo") },
                onFailure = { ActionState.Error(it.message ?: "Unknown error") }
            )
    }

    fun simCommentByU8() = viewModelScope.launch {
        _simCommentState.value = ActionState.Running
        _simCommentState.value = runCatching { SeedRepository.simulateCommentByTestUser(8) }
            .fold(
                onSuccess = { ActionState.Success("Test User 8 commented on their post") },
                onFailure = { ActionState.Error(it.message ?: "Unknown error") }
            )
    }
}
