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

    val showResults = submitted.isNotBlank() && submitted == query

    val focusRequester    = remember { FocusRequester() }
    val keyboard          = LocalSoftwareKeyboardController.current
    val context           = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddFriendSearchViewModel.AddFriendEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

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
                onQueryChange  = viewModel::onQueryChange,
                focusRequester = focusRequester,
                placeholder    = "Email or username",
                onSearch       = {
                    keyboard?.hide()
                    if (NetworkUtil.isOnline(context)) {
                        viewModel.submit()
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("No internet connection. Please try again when online.")
                        }
                    }
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
                                            onClick = {
                                                // Sending a request is a write —
                                                // gate on network the same as
                                                // Submit so we don't silently
                                                // queue into the offline cache.
                                                if (NetworkUtil.isOnline(context)) {
                                                    viewModel.sendFriendRequest(user.id)
                                                } else {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("No internet connection. Please try again when online.")
                                                    }
                                                }
                                            }
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
        AddFriendSearchScreen(
            onCancel            = {},
            onOpenMemberProfile = {}
        )
    }
}
