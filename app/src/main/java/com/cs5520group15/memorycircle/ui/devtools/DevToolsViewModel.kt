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

    private val _usersState   = MutableStateFlow<ActionState>(ActionState.Idle)
    private val _postState    = MutableStateFlow<ActionState>(ActionState.Idle)
    private val _historyState = MutableStateFlow<ActionState>(ActionState.Idle)

    val usersState:   StateFlow<ActionState> = _usersState.asStateFlow()
    val postState:    StateFlow<ActionState> = _postState.asStateFlow()
    val historyState: StateFlow<ActionState> = _historyState.asStateFlow()

    fun seedUsers(appContext: Context) = viewModelScope.launch {
        _usersState.value = ActionState.Running
        _usersState.value = runCatching { SeedRepository.seedTestUsers(appContext) }
            .fold(
                onSuccess = { r ->
                    val tail = if (r.errors.isEmpty()) "" else " · ${r.errors.size} errors"
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
}
