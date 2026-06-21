/**
 * What: Jetpack Compose UI for the Add Friend Search screen — full-screen
 *       search overlay with a live TextField and Firestore-backed results.
 * Who:  Wired into the nav graph by MemoryCircleNavigation for the
 *       AddFriendSearch route; reached from AddFriendScreen via its
 *       onOpenSearch callback.
 * When: Composed when the user navigates to the AddFriendSearch route.
 */

package com.cs5520group15.memorycircle.ui.addfriendsearch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.data.NetworkUtil
import com.cs5520group15.memorycircle.model.Friend
import com.cs5520group15.memorycircle.ui.common.AcceptPill
import com.cs5520group15.memorycircle.ui.common.AvatarListRow
import com.cs5520group15.memorycircle.ui.common.EmptyHint
import com.cs5520group15.memorycircle.ui.common.LockedPill
import com.cs5520group15.memorycircle.ui.common.SearchFieldRow
import com.cs5520group15.memorycircle.ui.theme.*
import kotlinx.coroutines.launch

/**
 * What: Full-screen "add new friend" search overlay. Auto-focused TextField at
 *       the top with a Cancel button on the right; live results below. Each
 *       result row shows avatar + name + masked email and a trailing button
 *       whose state depends on whether the user is already a friend, has an
 *       Add write in flight, or is fresh:
 *         already a friend → "Added" (locked grey pill)
 *         add in flight    → "Adding..." (locked grey pill)
 *         fresh            → "Add" (interactive AccentGreen pill)
 *       Tapping the row body (not the button) opens that user's profile.
 *
 *       Submit gates on connectivity: tapping Search while offline shows a
 *       snackbar without firing the Firestore query, matching the pattern
 *       used across the other write surfaces.
 * Who: Called by MemoryCircleNavigation for the AddFriendSearch route.
 * When: Reached from the search bar on AddFriendScreen.
 */
@Composable
fun AddFriendSearchScreen(
    onCancel:            () -> Unit,
    onOpenMemberProfile: (String) -> Unit,
    viewModel:           AddFriendSearchViewModel = viewModel()
) {
    val query       by viewModel.query.collectAsStateWithLifecycle()
    val submitted   by viewModel.submittedQuery.collectAsStateWithLifecycle()
    val results     by viewModel.results.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val isAdding    by viewModel.isAdding.collectAsStateWithLifecycle()
    val friends     by viewModel.friends.collectAsStateWithLifecycle()
    val outgoing    by viewModel.outgoingRequests.collectAsStateWithLifecycle()
    val friendIds   = remember(friends) { friends.map { it.id }.toSet() }

    val context           = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddFriendSearchViewModel.AddFriendEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    AddFriendSearchContent(
        query             = query,
        submitted         = submitted,
        results           = results,
        isSearching       = isSearching,
        isAdding          = isAdding,
        friendIds         = friendIds,
        outgoing          = outgoing,
        snackbarHostState = snackbarHostState,
        onQueryChange     = viewModel::onQueryChange,
        onSubmit          = {
            if (NetworkUtil.isOnline(context)) {
                viewModel.submit()
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("No internet connection. Please try again when online.")
                }
            }
        },
        onCancel             = onCancel,
        onOpenMemberProfile  = onOpenMemberProfile,
        onSendFriendRequest  = { userId ->
            // Sending a request is a write — gate on network the same as
            // Submit so we don't silently queue into the offline cache.
            if (NetworkUtil.isOnline(context)) {
                viewModel.sendFriendRequest(userId)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("No internet connection. Please try again when online.")
                }
            }
        }
    )
}

/**
 * Stateless content — takes plain values + callbacks so it renders in @Preview
 * without touching Firebase. The stateful AddFriendSearchScreen above is the
 * thin wrapper that wires the ViewModel and connectivity checks.
 */
@Composable
private fun AddFriendSearchContent(
    query:               String,
    submitted:           String,
    results:             List<Friend>,
    isSearching:         Boolean,
    isAdding:            Set<String>,
    friendIds:           Set<String>,
    outgoing:            Set<String>,
    snackbarHostState:   SnackbarHostState,
    onQueryChange:       (String) -> Unit,
    onSubmit:            () -> Unit,
    onCancel:            () -> Unit,
    onOpenMemberProfile: (String) -> Unit,
    onSendFriendRequest: (String) -> Unit
) {
    val showResults    = submitted.isNotBlank() && submitted == query
    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        containerColor = Cream,
        snackbarHost   = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SearchFieldRow(
                query          = query,
                onQueryChange  = onQueryChange,
                focusRequester = focusRequester,
                placeholder    = "Email or username",
                onSearch       = {
                    keyboard?.hide()
                    onSubmit()
                },
                onCancel       = {
                    keyboard?.hide()
                    onCancel()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isSearching -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Brown)
                    }
                }
                !showResults -> {
                    EmptyHint(text = "Search by email or username, then press Search.")
                }
                results.isEmpty() -> {
                    EmptyHint(text = "No users matched \"${submitted.trim()}\".")
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(results, key = { it.id }) { user ->
                            AvatarListRow(
                                name     = user.name,
                                subtitle = user.email,
                                photoUrl = user.avatarUrl,
                                onClick  = { onOpenMemberProfile(user.id) },
                                trailing = {
                                    when {
                                        user.id in friendIds  -> LockedPill(label = "Added")
                                        user.id in outgoing   -> LockedPill(label = "Invitation sent")
                                        user.id in isAdding   -> LockedPill(label = "Sending…")
                                        else                  -> AcceptPill(
                                            label   = "Add",
                                            onClick = { onSendFriendRequest(user.id) }
                                        )
                                    }
                                }
                            )
                            HorizontalDivider(color = Beige.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun AddFriendSearchScreenPreview() {
    MemoryCircleTheme {
        AddFriendSearchContent(
            query             = "ada",
            submitted         = "ada",
            results           = listOf(
                Friend(
                    id = "u1",
                    name = "Ada Lovelace",
                    email = "a***@example.com",
                    sharedMemories = 0,
                    isOnline = false,
                    avatarUrl = "",
                    bio = ""
                ),
                Friend(
                    id = "u2",
                    name = "Adam Smith",
                    email = "a***@example.com",
                    sharedMemories = 0,
                    isOnline = false,
                    avatarUrl = "",
                    bio = ""
                )
            ),
            isSearching       = false,
            isAdding          = emptySet(),
            friendIds         = setOf("u2"),
            outgoing          = emptySet(),
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange       = {},
            onSubmit            = {},
            onCancel            = {},
            onOpenMemberProfile = {},
            onSendFriendRequest = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun AddFriendSearchScreenEmptyPreview() {
    MemoryCircleTheme {
        AddFriendSearchContent(
            query             = "",
            submitted         = "",
            results           = emptyList(),
            isSearching       = false,
            isAdding          = emptySet(),
            friendIds         = emptySet(),
            outgoing          = emptySet(),
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange       = {},
            onSubmit            = {},
            onCancel            = {},
            onOpenMemberProfile = {},
            onSendFriendRequest = {}
        )
    }
}
