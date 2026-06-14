package com.cs5520group15.memorycircle.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * What: Full-screen search overlay for the Friends tab. Top: a focused search
 *       field + Cancel. Below: when the query is blank, a 2-column grid of
 *       recent searches (collapsed to 6, expandable to all 10); when typing,
 *       live results split into FRIENDS (matches name or email) and GROUPS
 *       (matches name). Tapping a result commits the query to history, then
 *       navigates — friends to their profile (placeholder), groups to GroupDetail.
 * Who: Called by MemoryCircleNavigation for the FriendsSearch route.
 * When: Opened by tapping the search bar on FriendsScreen.
 *
 * @param onCancel             pops back to FriendsScreen
 * @param onOpenMemberProfile  navigates to a friend's profile (placeholder route)
 * @param onOpenGroupDetail    navigates to GroupDetail for the tapped group
 */
@Composable
fun FriendsSearchScreen(
    onCancel:             () -> Unit,
    onOpenMemberProfile:  (String) -> Unit,
    onOpenGroupDetail:    (String) -> Unit,
    viewModel:            FriendsSearchViewModel = viewModel()
) {
    val query    by viewModel.query.collectAsStateWithLifecycle()
    val recent   by viewModel.recent.collectAsStateWithLifecycle()

    // Derived results — recomputed on each recomposition while the user types.
    val friendResults = viewModel.matchFriends(query)
    val groupResults  = viewModel.matchGroups(query)

    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current

    // Auto-focus the field on entry so the keyboard rises immediately, matching
    // the WeChat-style search experience the user pointed at.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(containerColor = Cream) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SearchFieldRow(
                query           = query,
                onQueryChange   = viewModel::onQueryChange,
                focusRequester  = focusRequester,
                onSearch        = { keyboard?.hide() },
                onCancel        = {
                    keyboard?.hide()
                    onCancel()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (query.isBlank()) {
                RecentSearches(
                    recent       = recent,
                    onRecentTap  = { viewModel.onQueryChange(it) }
                )
            } else {
                ResultsList(
                    friends             = friendResults,
                    groups              = groupResults,
                    onFriendTap         = { friend ->
                        viewModel.commitQueryToHistory()
                        onOpenMemberProfile(friend.id)
                    },
                    onGroupTap          = { group ->
                        viewModel.commitQueryToHistory()
                        onOpenGroupDetail(group.id)
                    }
                )
            }
        }
    }
}

/**
 * What: Top row — auto-focused search field on the left, "Cancel" text button
 *       on the right that pops back to the previous screen.
 * Who: Called by FriendsSearchScreen.
 * When: Rendered once at the top of the page.
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
                    text  = "Search friends or groups",
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
            // Match the soft keyboard's enter key to the screen's intent —
            // pressing it tucks the keyboard away (results are already live
            // as the user types, so there's nothing extra to commit).
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
 * What: Recent-search grid — section label + expand toggle on the right, then
 *       a 2-column chunked grid of recent query chips. The grid is capped at
 *       the first 6 (= 2×3) when collapsed; tapping "Expand" reveals all 10.
 *       Tapping a chip re-fills the search field (which then shows results).
 * Who: Called by FriendsSearchScreen when the query is blank.
 * When: Rendered while the user has cleared the search field.
 */
@Composable
private fun RecentSearches(
    recent:      List<String>,
    onRecentTap: (String) -> Unit
) {
    if (recent.isEmpty()) {
        Text(
            text  = "Your recent searches will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkTertiary,
            modifier = Modifier.padding(top = 8.dp)
        )
        return
    }

    var expanded by remember { mutableStateOf(false) }
    val collapsedCount = 6
    val visible = if (expanded) recent else recent.take(collapsedCount)
    val canExpand = recent.size > collapsedCount

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = "RECENT SEARCHES",
                style = MaterialTheme.typography.labelSmall,
                color = Ink
            )
            if (canExpand) {
                TextButton(
                    onClick        = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text  = if (expanded) "Collapse  ▴" else "Expand  ▾",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Brown
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            visible.chunked(2).forEach { rowItems ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { term ->
                        Box(modifier = Modifier.weight(1f)) {
                            RecentChip(text = term, onClick = { onRecentTap(term) })
                        }
                    }
                    // Pad to 2 columns if the last row only has 1 item.
                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * What: One recent-search chip — pill-shaped tappable label that refills the
 *       search field with the chip's text.
 * Who: Called by RecentSearches.
 * When: Rendered for every visible recent term.
 */
@Composable
private fun RecentChip(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Beige.copy(alpha = 0.35f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text     = text,
            style    = MaterialTheme.typography.bodyMedium,
            color    = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * What: Live results — FRIENDS section (matches on name or email) then GROUPS
 *       section (matches on name). If both are empty we show a single
 *       "No results" line so the screen never looks broken.
 * Who: Called by FriendsSearchScreen when the query is non-blank.
 * When: Rendered while the user is typing.
 */
@Composable
private fun ResultsList(
    friends:      List<Friend>,
    groups:       List<GroupSummary>,
    onFriendTap:  (Friend) -> Unit,
    onGroupTap:   (GroupSummary) -> Unit
) {
    if (friends.isEmpty() && groups.isEmpty()) {
        Text(
            text  = "No results found.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkTertiary,
            modifier = Modifier.padding(top = 8.dp)
        )
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (friends.isNotEmpty()) {
            item {
                SectionLabel(label = "FRIENDS", count = friends.size)
            }
            items(friends, key = { "f_${it.id}" }) { friend ->
                FriendResultRow(friend = friend, onClick = { onFriendTap(friend) })
                HorizontalDivider(color = Beige.copy(alpha = 0.5f))
            }
        }
        if (groups.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionLabel(label = "GROUPS", count = groups.size)
            }
            items(groups, key = { "g_${it.id}" }) { group ->
                GroupResultRow(group = group, onClick = { onGroupTap(group) })
                HorizontalDivider(color = Beige.copy(alpha = 0.5f))
            }
        }
    }
}

/**
 * What: Tiny labelled header used above the FRIENDS / GROUPS result blocks.
 * Who: Called by ResultsList.
 * When: Rendered above each non-empty result block.
 */
@Composable
private fun SectionLabel(label: String, count: Int) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = Ink
        )
        Text(
            text  = "$count",
            style = MaterialTheme.typography.bodyMedium,
            color = Brown
        )
    }
}

/**
 * What: One friend search result — avatar, name, and the email shown beneath
 *       (so the user can verify a match when they typed an email).
 * Who: Called by ResultsList.
 * When: Rendered for every friend match.
 */
@Composable
private fun FriendResultRow(friend: Friend, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(name = friend.name, size = 44.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = friend.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Ink
            )
            Text(
                text     = friend.email,
                style    = MaterialTheme.typography.bodyMedium,
                color    = InkTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * What: One group search result — a small color chip, the group name, and
 *       a "N members" sub-line. Tapping opens GroupDetail.
 * Who: Called by ResultsList.
 * When: Rendered for every group match.
 */
@Composable
private fun GroupResultRow(group: GroupSummary, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Sage.copy(alpha = 0.7f))
                .border(1.dp, Sage, RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = group.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Ink
            )
            Text(
                text  = "${group.memberCount} members",
                style = MaterialTheme.typography.bodyMedium,
                color = InkTertiary
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun FriendsSearchScreenPreview() {
    MemoryCircleTheme {
        FriendsSearchScreen(
            onCancel             = {},
            onOpenMemberProfile  = {},
            onOpenGroupDetail    = {}
        )
    }
}
