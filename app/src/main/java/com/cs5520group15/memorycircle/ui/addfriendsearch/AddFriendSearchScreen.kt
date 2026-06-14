package com.cs5520group15.memorycircle.ui.addfriendsearch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.ui.common.AcceptPill
import com.cs5520group15.memorycircle.ui.common.AvatarListRow
import com.cs5520group15.memorycircle.ui.common.EmptyHint
import com.cs5520group15.memorycircle.ui.common.LockedPill
import com.cs5520group15.memorycircle.ui.common.SearchFieldRow
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Full-screen "add new friend" search overlay. Auto-focused TextField at
 *       the top with a Cancel button on the right; live results below grouped
 *       into one flat list (no "All / People / Groups" tabs — this flow is
 *       people-only per the spec). Each result row shows avatar + name +
 *       email and a trailing button whose state depends on whether the user
 *       is already a friend, has been invited, or is fresh:
 *         already a friend → "Added" (locked grey pill)
 *         invited          → "Invitation sent" (locked grey pill)
 *         fresh            → "Add" (interactive AccentGreen pill)
 *       Tapping the row body (not the button) opens that user's profile.
 * Who: Called by MemoryCircleNavigation for the AddFriendSearch route.
 * When: Reached from the search bar on AddFriendScreen.
 */
@Composable
fun AddFriendSearchScreen(
    onCancel:            () -> Unit,
    onOpenMemberProfile: (String) -> Unit,
    viewModel:           AddFriendSearchViewModel = viewModel()
) {
    val query     by viewModel.query.collectAsStateWithLifecycle()
    val submitted by viewModel.submittedQuery.collectAsStateWithLifecycle()
    val friends   by viewModel.friends.collectAsStateWithLifecycle()
    val invited   by viewModel.invited.collectAsStateWithLifecycle()
    val friendIds = remember(friends) { friends.map { it.id }.toSet() }

    val showResults = submitted.isNotBlank() && submitted == query
    val results = if (showResults) viewModel.match(submitted) else emptyList()

    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(containerColor = Cream) { padding ->
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
                    viewModel.submit()
                    keyboard?.hide()
                },
                onCancel       = {
                    keyboard?.hide()
                    onCancel()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
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
                                onClick  = { onOpenMemberProfile(user.id) },
                                trailing = {
                                    when {
                                        user.id in friendIds -> LockedPill(label = "Added")
                                        user.id in invited   -> LockedPill(label = "Invitation sent")
                                        else                 -> AcceptPill(
                                            label   = "Add",
                                            onClick = { viewModel.invite(user.id) }
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
