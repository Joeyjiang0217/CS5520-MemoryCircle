/**
 * What: Jetpack Compose UI for the Friends Search screen — full-screen search
 *       over the user's existing friends and groups.
 * Who:  Wired into the nav graph by MemoryCircleNavigation for the FriendsSearch
 *       route; reached from FriendsScreen via its onOpenSearch callback.
 * When: Composed when the user navigates to the FriendsSearch route.
 */

package com.cs5520group15.memorycircle.ui.friendsearch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.model.Friend
import com.cs5520group15.memorycircle.model.GroupSummary
import com.cs5520group15.memorycircle.ui.common.AvatarListRow
import com.cs5520group15.memorycircle.ui.common.EmptyHint
import com.cs5520group15.memorycircle.ui.common.GroupRow
import com.cs5520group15.memorycircle.ui.common.SearchFieldRow
import com.cs5520group15.memorycircle.ui.common.SectionHeader
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

    val friendResults = viewModel.matchFriends(query)
    val groupResults  = viewModel.matchGroups(query)

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
                query           = query,
                onQueryChange   = viewModel::onQueryChange,
                focusRequester  = focusRequester,
                placeholder     = "Search friends or groups",
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
        EmptyHint(text = "Your recent searches will show up here.")
        return
    }

    var expanded by remember { mutableStateOf(false) }
    val collapsedCount = 6
    val visible = if (expanded) recent else recent.take(collapsedCount)
    val canExpand = recent.size > collapsedCount

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            text = "RECENT SEARCHES",
            trailing = {
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
        )

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
                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

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
        EmptyHint(text = "No results found.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (friends.isNotEmpty()) {
            item {
                SectionHeader(
                    text = "FRIENDS",
                    trailing = {
                        Text(
                            text  = "${friends.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Brown
                        )
                    }
                )
            }
            items(friends, key = { "f_${it.id}" }) { friend ->
                AvatarListRow(
                    name     = friend.name,
                    subtitle = friend.email,
                    photoUrl = friend.avatarUrl,
                    onClick  = { onFriendTap(friend) }
                )
                HorizontalDivider(color = Beige.copy(alpha = 0.5f))
            }
        }
        if (groups.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    text = "GROUPS",
                    trailing = {
                        Text(
                            text  = "${groups.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Brown
                        )
                    }
                )
            }
            items(groups, key = { "g_${it.id}" }) { group ->
                GroupRow(
                    name        = group.name,
                    memberCount = group.memberCount,
                    onClick     = { onGroupTap(group) },
                    bordered    = true
                )
            }
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

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun RecentSearchesPreview() {
    MemoryCircleTheme {
        RecentSearches(
            recent = listOf("ada", "grace"),
            onRecentTap = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun RecentChipPreview() {
    MemoryCircleTheme {
        RecentChip(
            text = "ada",
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun ResultsListPreview() {
    MemoryCircleTheme {
        ResultsList(
            friends = listOf(
                Friend(
                    id = "u1",
                    name = "Ada Lovelace",
                    email = "ada@example.com",
                    sharedMemories = 4,
                    isOnline = true,
                    avatarUrl = "",
                    bio = ""
                )
            ),
            groups = listOf(
                GroupSummary(
                    id = "g1",
                    name = "Summer Trip",
                    memberCount = 5,
                    memberAvatarUrls = emptyList(),
                    memberNames = listOf("Ada", "Grace")
                )
            ),
            onFriendTap = {},
            onGroupTap = {}
        )
    }
}
