package com.cs5520group15.memorycircle.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
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
 *       Tapping the row body (not the button) opens that user's profile
 *       (placeholder until the profile screen lands).
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

    // Results are derived from `submitted` only, not the live `query`.
    // We additionally require submitted == query: once the user edits the field
    // again, the previous result set should disappear immediately so it can't
    // be mistaken for a fresh result for the new typing.
    val showResults = submitted.isNotBlank() && submitted == query
    val results = if (showResults) viewModel.match(submitted) else emptyList()

    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current

    // Pop the keyboard immediately on entry so the user can start typing
    // without an extra tap, matching the WeChat-style flow they referenced.
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
                    Hint(text = "Search by email or username, then press Search.")
                }
                results.isEmpty() -> {
                    Hint(text = "No users matched \"${submitted.trim()}\".")
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(results, key = { it.id }) { user ->
                            val state = when {
                                user.id in friendIds -> ResultState.ALREADY_FRIEND
                                user.id in invited   -> ResultState.INVITED
                                else                 -> ResultState.FRESH
                            }
                            UserResultRow(
                                user    = user,
                                state   = state,
                                onAdd   = { viewModel.invite(user.id) },
                                onClick = { onOpenMemberProfile(user.id) }
                            )
                            HorizontalDivider(color = Beige.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * What: Per-result state used to pick the button visual on the right side of
 *       a row. Kept private to this file since it's purely a rendering hint.
 */
private enum class ResultState { FRESH, INVITED, ALREADY_FRIEND }

/**
 * What: Top row — auto-focused TextField on the left (placeholder spells out
 *       the two accepted query types per the spec), Cancel text button on the
 *       right that pops back to AddFriendScreen.
 * Who: Called by AddFriendSearchScreen.
 * When: Rendered once at the top.
 */
@Composable
private fun SearchFieldRow(
    query:          String,
    onQueryChange:  (String) -> Unit,
    focusRequester: FocusRequester,
    onSearch:       () -> Unit,
    onCancel:       () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value         = query,
            onValueChange = onQueryChange,
            modifier      = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine    = true,
            shape         = RoundedCornerShape(24.dp),
            placeholder   = {
                Text(
                    text  = "Email or username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTertiary
                )
            },
            leadingIcon   = {
                Icon(
                    painter            = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint               = Brown
                )
            },
            // Soft keyboard's enter key becomes a Search action so the icon
            // matches what the press actually does (commit the query, not
            // insert a newline).
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Sage,
                unfocusedBorderColor = Beige
            )
        )
        TextButton(onClick = onCancel) {
            Text(
                text  = "Cancel",
                style = MaterialTheme.typography.labelLarge,
                color = AccentGreen
            )
        }
    }
}

/**
 * What: One result row — avatar, name + email beneath, and a trailing button
 *       whose visual is dictated by ResultState. Tapping the row body opens
 *       the user's profile; tapping the trailing button performs (or
 *       advertises) the friend-add action.
 * Who: Called by AddFriendSearchScreen for each match.
 * When: Rendered for every match in the results list.
 */
@Composable
private fun UserResultRow(
    user:    Friend,
    state:   ResultState,
    onAdd:   () -> Unit,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        AvatarCircle(name = user.name, size = 44.dp)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = user.name,
                style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color    = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text     = user.email,
                style    = MaterialTheme.typography.bodyMedium,
                color    = InkTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        when (state) {
            ResultState.FRESH          -> AddPill(onClick = onAdd)
            ResultState.INVITED        -> LockedPill(label = "Invitation sent")
            ResultState.ALREADY_FRIEND -> LockedPill(label = "Added")
        }
    }
}

/**
 * What: Interactive Add pill — AccentGreen bg / Cream text. Sole entry point
 *       for the invite mutation on this screen.
 * Who: Called by UserResultRow for FRESH state.
 * When: Rendered when the user is neither a friend nor already invited.
 */
@Composable
private fun AddPill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AccentGreen)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text  = "Add",
            style = MaterialTheme.typography.labelLarge,
            color = Cream
        )
    }
}

/**
 * What: Locked pill for terminal states ("Invitation sent" / "Added") —
 *       BrownDisabled bg, InkSecondary text, non-interactive. Same visual
 *       treatment as AllFriendRequestsScreen.StatusPill so terminal states
 *       across the app stay coherent.
 * Who: Called by UserResultRow for INVITED and ALREADY_FRIEND states.
 * When: Rendered when the action no longer applies.
 */
@Composable
private fun LockedPill(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BrownDisabled)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge,
            color = InkSecondary
        )
    }
}

/**
 * What: Empty / no-results hint line, sized to match the bodyMedium copy
 *       used elsewhere for placeholder content.
 * Who: Called by AddFriendSearchScreen.
 * When: Rendered when the query is blank or no users match the query.
 */
@Composable
private fun Hint(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.bodyMedium,
        color = InkTertiary,
        modifier = Modifier.padding(top = 8.dp)
    )
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
