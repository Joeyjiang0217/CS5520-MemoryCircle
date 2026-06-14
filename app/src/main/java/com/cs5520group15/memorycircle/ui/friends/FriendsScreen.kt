package com.cs5520group15.memorycircle.ui.friends

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.model.Friend
import com.cs5520group15.memorycircle.model.FriendRequest
import com.cs5520group15.memorycircle.model.GroupSummary
import com.cs5520group15.memorycircle.ui.common.AcceptPill
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.AvatarListRow
import com.cs5520group15.memorycircle.ui.common.DeclineCircleButton
import com.cs5520group15.memorycircle.ui.common.EmptyHint
import com.cs5520group15.memorycircle.ui.common.GroupRow
import com.cs5520group15.memorycircle.ui.common.MemoryCircleBottomNav
import com.cs5520group15.memorycircle.ui.common.SectionHeader
import com.cs5520group15.memorycircle.ui.common.TapSearchBar
import com.cs5520group15.memorycircle.ui.theme.*
import kotlinx.coroutines.launch

/**
 * What: Friends tab landing screen — page header, search bar, friend-requests
 *       card, and a sticky Friends / Groups switcher below them. The switcher
 *       pins to the top once the user scrolls past it so the user can flip
 *       tabs without scrolling back up. Friends list is alphabetised with an
 *       A-Z index column floating on the right; Groups list is a plain feed.
 * Who: Called by MemoryCircleNavigation for the Friends route.
 * When: Displayed when the user taps the Friends tab.
 *
 * @param onOpenSearch         opens the full-screen friend/group search overlay
 * @param onOpenAllRequests    opens the full pending-requests list
 * @param onOpenMemberProfile  navigates to a friend's profile (placeholder route)
 * @param onOpenGroupDetail    navigates to GroupDetail for the tapped group
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendsScreen(
    currentRoute:        String,
    onNavigate:          (Any) -> Unit,
    onOpenSearch:        () -> Unit,
    onOpenAllRequests:   () -> Unit,
    onOpenAddFriend:     () -> Unit,
    onOpenMemberProfile: (String) -> Unit,
    onOpenGroupDetail:   (String) -> Unit,
    viewModel:           FriendsViewModel = viewModel()
) {
    val friends     by viewModel.friends.collectAsStateWithLifecycle()
    val groups      by viewModel.groups.collectAsStateWithLifecycle()
    val allRequests by viewModel.requests.collectAsStateWithLifecycle()
    val pendingRequests = allRequests.filter { it.status == FriendRequest.Status.PENDING }

    val friendSections: List<Pair<Char, List<Friend>>> = remember(friends) {
        friends
            .sortedWith(compareBy({ letterKeyOf(it.name) }, { it.name.lowercase() }))
            .groupBy { letterKeyOf(it.name) }
            .toList()
            .sortedWith(compareBy { sectionRank(it.first) })
    }

    var selectedTab by rememberSaveable { mutableStateOf(ContactsTab.FRIENDS) }

    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    val tabBarHeightPx = with(LocalDensity.current) { 54.dp.roundToPx() }

    val hasRequests = pendingRequests.isNotEmpty()
    val tabBarIndex = remember(hasRequests) { if (hasRequests) 3 else 2 }

    val sectionIndices: Map<Char, Int> = remember(friendSections, tabBarIndex) {
        val map = mutableMapOf<Char, Int>()
        var idx = tabBarIndex + 1
        friendSections.forEach { (letter, list) ->
            map[letter] = idx
            idx += 1 + list.size
        }
        map
    }

    var initialMount by remember { mutableStateOf(true) }
    LaunchedEffect(selectedTab) {
        if (initialMount) {
            initialMount = false
        } else if (listState.firstVisibleItemIndex >= tabBarIndex) {
            listState.animateScrollToItem(tabBarIndex)
        }
    }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            MemoryCircleBottomNav(
                currentRoute = currentRoute,
                onNavigate   = onNavigate
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state               = listState,
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item(key = "header") {
                    Header(
                        friendCount    = friends.size,
                        groupCount     = groups.size,
                        onAddFriendTap = onOpenAddFriend,
                        modifier       = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item(key = "search") {
                    TapSearchBar(
                        placeholder = "Search friends…",
                        onClick     = onOpenSearch,
                        modifier    = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (hasRequests) {
                    item(key = "requests") {
                        FriendRequestsSection(
                            pending   = pendingRequests,
                            onAccept  = viewModel::acceptRequest,
                            onReject  = viewModel::rejectRequest,
                            onOpenAll = onOpenAllRequests,
                            modifier  = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                stickyHeader(key = "tabs") {
                    ContactsTabRow(
                        selected   = selectedTab,
                        onSelect   = { selectedTab = it }
                    )
                }

                when (selectedTab) {
                    ContactsTab.FRIENDS -> {
                        if (friendSections.isEmpty()) {
                            item(key = "friends_empty") {
                                EmptyHint(text = "No friends yet.")
                            }
                        }
                        friendSections.forEach { (letter, list) ->
                            item(key = "section_$letter") { SectionLetter(letter) }
                            items(list, key = { "friend_${it.id}" }) { friend ->
                                AvatarListRow(
                                    name     = friend.name,
                                    subtitle = "${friend.sharedMemories} shared memories",
                                    isOnline = friend.isOnline,
                                    onClick  = { onOpenMemberProfile(friend.id) },
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    trailing = {
                                        if (friend.isOnline) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Sage.copy(alpha = 0.35f))
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text  = "Active",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AccentGreen
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        item(key = "friends_pad") {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                    ContactsTab.GROUPS -> {
                        if (groups.isEmpty()) {
                            item(key = "groups_empty") {
                                EmptyHint(text = "No groups yet.")
                            }
                        }
                        items(groups, key = { "group_${it.id}" }) { group ->
                            GroupRow(
                                name        = group.name,
                                memberCount = group.memberCount,
                                onClick     = { onOpenGroupDetail(group.id) },
                                modifier    = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                        item(key = "groups_pad") {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            if (selectedTab == ContactsTab.FRIENDS && friendSections.isNotEmpty()) {
                AlphabetIndex(
                    availableLetters = sectionIndices.keys,
                    onLetterTap      = { letter ->
                        sectionIndices[letter]?.let { idx ->
                            scope.launch {
                                listState.animateScrollToItem(
                                    index        = idx,
                                    scrollOffset = -tabBarHeightPx
                                )
                            }
                        }
                    },
                    onStarTap        = {
                        scope.launch { listState.animateScrollToItem(tabBarIndex) }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                )
            }
        }
    }
}

private fun letterKeyOf(name: String): Char {
    val first = name.firstOrNull()?.uppercaseChar() ?: '#'
    return if (first in 'A'..'Z') first else '#'
}

private fun sectionRank(letter: Char): Int = if (letter == '#') Int.MAX_VALUE else letter.code

private enum class ContactsTab { FRIENDS, GROUPS }

@Composable
private fun Header(
    friendCount:    Int,
    groupCount:     Int,
    onAddFriendTap: () -> Unit,
    modifier:       Modifier = Modifier
) {
    Row(
        modifier          = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = "Friends & Groups",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "$friendCount friends · $groupCount groups",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
        }
        IconButton(onClick = onAddFriendTap) {
            Icon(
                painter            = painterResource(R.drawable.ic_personadd),
                contentDescription = "Add new friend",
                tint               = Ink
            )
        }
    }
}

@Composable
private fun FriendRequestsSection(
    pending:   List<FriendRequest>,
    onAccept:  (String) -> Unit,
    onReject:  (String) -> Unit,
    onOpenAll: () -> Unit,
    modifier:  Modifier = Modifier
) {
    if (pending.isEmpty()) return
    val newest = pending.first()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            text = "FRIEND REQUESTS (${pending.size})",
            trailing = {
                TextButton(
                    onClick        = onOpenAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text  = "See all  ›",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Brown
                    )
                }
            }
        )

        FriendRequestCard(
            request  = newest,
            onAccept = { onAccept(newest.id) },
            onReject = { onReject(newest.id) }
        )
    }
}

/**
 * What: A single Friend Request card — Sage-tinted container wrapping an
 *       AvatarListRow with Accept + reject ✕ in the trailing slot.
 * Who: Called by FriendRequestsSection.
 * When: Rendered for the head-of-queue request.
 */
@Composable
private fun FriendRequestCard(
    request:  FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Sage.copy(alpha = 0.18f))
            .border(1.dp, Sage.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarCircle(name = request.fromUserName, size = 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = request.fromUserName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Ink
                )
                Text(
                    text  = if (request.mutualFriends > 0)
                                "${request.mutualFriends} mutual friend${if (request.mutualFriends == 1) "" else "s"}"
                            else
                                request.fromUserEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTertiary
                )
            }
            AcceptPill(label = "Accept", onClick = onAccept)
            Spacer(modifier = Modifier.width(6.dp))
            DeclineCircleButton(onClick = onReject)
        }
    }
}

@Composable
private fun ContactsTabRow(
    selected: ContactsTab,
    onSelect: (ContactsTab) -> Unit
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Cream)
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        TabItem(
            label    = "Friends",
            selected = selected == ContactsTab.FRIENDS,
            onClick  = { onSelect(ContactsTab.FRIENDS) }
        )
        TabItem(
            label    = "Groups",
            selected = selected == ContactsTab.GROUPS,
            onClick  = { onSelect(ContactsTab.GROUPS) }
        )
    }
}

@Composable
private fun TabItem(
    label:    String,
    selected: Boolean,
    onClick:  () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (selected) Ink else InkTertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(if (selected) 28.dp else 0.dp)
                .height(2.dp)
                .background(Brown)
        )
    }
}

@Composable
private fun SectionLetter(letter: Char) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cream)
            .padding(horizontal = 24.dp, vertical = 6.dp)
    ) {
        Text(
            text  = letter.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = InkSecondary
        )
    }
}

@Composable
private fun AlphabetIndex(
    availableLetters: Set<Char>,
    onLetterTap:      (Char) -> Unit,
    onStarTap:        () -> Unit,
    modifier:         Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Cream.copy(alpha = 0.75f))
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        IndexEntry(label = "★", enabled = true, onClick = onStarTap)
        ('A'..'Z').forEach { letter ->
            val enabled = letter in availableLetters
            IndexEntry(
                label   = letter.toString(),
                enabled = enabled,
                onClick = { if (enabled) onLetterTap(letter) }
            )
        }
        val hashEnabled = '#' in availableLetters
        IndexEntry(
            label   = "#",
            enabled = hashEnabled,
            onClick = { if (hashEnabled) onLetterTap('#') }
        )
    }
}

@Composable
private fun IndexEntry(
    label:   String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 18.dp, height = 16.dp)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Text(
            text     = label,
            fontSize = 10.sp,
            color    = if (enabled) Brown else BrownDisabled
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun FriendsScreenPreview() {
    MemoryCircleTheme {
        FriendsScreen(
            currentRoute        = "friends",
            onNavigate          = {},
            onOpenSearch        = {},
            onOpenAllRequests   = {},
            onOpenAddFriend     = {},
            onOpenMemberProfile = {},
            onOpenGroupDetail   = {}
        )
    }
}
