package com.cs5520group15.memorycircle.ui.friends

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.MemoryCircleBottomNav
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
    onOpenMemberProfile: (String) -> Unit,
    onOpenGroupDetail:   (String) -> Unit,
    viewModel:           FriendsViewModel = viewModel()
) {
    val friends     by viewModel.friends.collectAsStateWithLifecycle()
    val groups      by viewModel.groups.collectAsStateWithLifecycle()
    val allRequests by viewModel.requests.collectAsStateWithLifecycle()
    // Only PENDING requests belong on this screen — accepted/declined entries
    // live on the "See all" page so the user can still see their history there.
    val pendingRequests = allRequests.filter { it.status == FriendRequest.Status.PENDING }

    // Friends grouped by first-letter section, then alphabetised within section.
    // Non-letter starts (numbers, CJK, etc.) bucket into '#' and sort last.
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

    // Tab bar height in pixels — used as a negative scrollOffset when jumping
    // to a section, so the section letter lands JUST BELOW the sticky tab bar
    // instead of being hidden behind it. ~54dp matches ContactsTabRow's layout
    // (Cormorant title + 4dp gap + 2dp underline + 10dp vertical padding × 2).
    val tabBarHeightPx = with(LocalDensity.current) { 54.dp.roundToPx() }

    // Item-index bookkeeping. The LazyColumn layout is built in this exact order
    // so the indices below match — keep this in sync if you add/remove items.
    val hasRequests = pendingRequests.isNotEmpty()
    val tabBarIndex = remember(hasRequests) { if (hasRequests) 3 else 2 }

    // letter → LazyColumn item index for the alphabet jump bar.
    val sectionIndices: Map<Char, Int> = remember(friendSections, tabBarIndex) {
        val map = mutableMapOf<Char, Int>()
        var idx = tabBarIndex + 1  // first item after the (sticky) tab bar
        friendSections.forEach { (letter, list) ->
            map[letter] = idx
            idx += 1 + list.size  // section header + its friend rows
        }
        map
    }

    // When switching tabs while scrolled, snap to the sticky tab bar so the new
    // tab's content reads from the top. Skip the first composition — at app start
    // selectedTab "changes" from null to FRIENDS and we don't want to scroll then.
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
                        friendCount = friends.size,
                        groupCount  = groups.size,
                        modifier    = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item(key = "search") {
                    SearchBar(
                        onClick  = onOpenSearch,
                        modifier = Modifier.padding(horizontal = 24.dp)
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
                                FriendRow(
                                    friend  = friend,
                                    onClick = { onOpenMemberProfile(friend.id) }
                                )
                            }
                        }
                        // Bottom padding so the last row isn't flush with the bottom nav.
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
                                group   = group,
                                onClick = { onOpenGroupDetail(group.id) }
                            )
                        }
                        item(key = "groups_pad") {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            // Alphabet jump bar — only meaningful on the FRIENDS tab.
            if (selectedTab == ContactsTab.FRIENDS && friendSections.isNotEmpty()) {
                AlphabetIndex(
                    availableLetters = sectionIndices.keys,
                    onLetterTap      = { letter ->
                        sectionIndices[letter]?.let { idx ->
                            scope.launch {
                                // Negative offset so the section letter ends up
                                // tabBarHeightPx below the viewport top — i.e. just
                                // under the sticky tab bar, not behind it.
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

/**
 * What: First-letter bucket key used to group friends. Latin letters bucket to
 *       their uppercase form; everything else (digits, CJK, symbols) falls into
 *       a single '#' bucket that sorts after Z.
 * Who: Called when building friendSections.
 * When: Per recomposition that re-derives the section list.
 */
private fun letterKeyOf(name: String): Char {
    val first = name.firstOrNull()?.uppercaseChar() ?: '#'
    return if (first in 'A'..'Z') first else '#'
}

/**
 * What: Lexical rank that keeps '#' at the bottom of the section list while
 *       'A'..'Z' sort in their natural order.
 * Who: Called by the section sorter on FriendsScreen.
 * When: Per recomposition that re-derives the section list.
 */
private fun sectionRank(letter: Char): Int = if (letter == '#') Int.MAX_VALUE else letter.code

/**
 * What: Two-tab switcher between the friend list and the group list. Exists as
 *       an enum (rather than a Boolean) so the call sites read cleanly and
 *       further tabs (e.g. Channels) can be added without re-typing the API.
 * Who: Local UI state on FriendsScreen.
 * When: Mutated when the user taps a label in ContactsTabRow.
 */
private enum class ContactsTab { FRIENDS, GROUPS }

/**
 * What: Hero header — large serif "Friends & Groups" and the "N friends · M groups"
 *       sub-line that lives just under it.
 * Who: Called by FriendsScreen.
 * When: Rendered once at the top of the page.
 */
@Composable
private fun Header(
    friendCount: Int,
    groupCount:  Int,
    modifier:    Modifier = Modifier
) {
    Column(modifier = modifier) {
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
}

/**
 * What: A tap-only search bar — looks like an input field but the whole row is
 *       a clickable target that opens the search overlay.
 * Who: Called by FriendsScreen.
 * When: Rendered once below the header.
 */
@Composable
private fun SearchBar(
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(WhiteCard)
            .border(1.dp, Beige.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Icon(
            painter            = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint               = Brown
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text  = "Search friends…",
            style = MaterialTheme.typography.bodyLarge,
            color = InkTertiary
        )
    }
}

/**
 * What: "FRIEND REQUESTS" section — a labelled header with a "See all" link on
 *       the right and a single card showing the head-of-queue request.
 * Who: Called by FriendsScreen.
 * When: Rendered whenever there is at least one PENDING request.
 */
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
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = "FRIEND REQUESTS (${pending.size})",
                style = MaterialTheme.typography.labelSmall,
                color = Ink
            )
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

        FriendRequestCard(
            request  = newest,
            onAccept = { onAccept(newest.id) },
            onReject = { onReject(newest.id) }
        )
    }
}

/**
 * What: A single Friend Request card — avatar (left), name + "N mutual friends"
 *       sub-line (middle), Accept pill + dismiss "✕" (right).
 * Who: Called by FriendRequestsSection.
 * When: Rendered for the head-of-queue request.
 */
@Composable
private fun FriendRequestCard(
    request:  FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Sage.copy(alpha = 0.18f))
            .border(1.dp, Sage.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
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

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(AccentGreen)
                .clickable { onAccept() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text  = "Accept",
                style = MaterialTheme.typography.labelLarge,
                color = Cream
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(GraySoft)
                .clickable { onReject() }
        ) {
            Text(
                text  = "✕",
                style = MaterialTheme.typography.labelLarge,
                color = InkSecondary
            )
        }
    }
}

/**
 * What: Sticky Friends / Groups switcher. Sits in the LazyColumn flow until it
 *       reaches the top of the viewport, then pins. Background is opaque so the
 *       list rows scrolling underneath don't bleed through.
 * Who: Called by FriendsScreen as a stickyHeader.
 * When: Always visible — either in flow or pinned at the top.
 */
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

/**
 * What: One tab label with an underline indicator that only shows when selected.
 * Who: Called by ContactsTabRow.
 * When: Rendered per tab.
 */
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

/**
 * What: Small uppercase section label (A, B, C, … or '#') that introduces
 *       each alphabetical chunk of the friend list.
 * Who: Called by FriendsScreen.
 * When: Rendered above each section's rows.
 */
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

/**
 * What: One friend row — avatar (with optional online dot), name, and the
 *       "N shared memories" sub-line that ties back to the GroupMembers screen.
 * Who: Called by FriendsScreen for every friend.
 * When: Rendered for every entry in the friend list.
 */
@Composable
private fun FriendRow(friend: Friend, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Box {
            AvatarCircle(name = friend.name, size = 44.dp)
            if (friend.isOnline) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = friend.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Ink
            )
            Text(
                text  = "${friend.sharedMemories} shared memories",
                style = MaterialTheme.typography.bodyMedium,
                color = InkTertiary
            )
        }

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
}

/**
 * What: One group row in the Groups tab — color chip, group name, "N members".
 * Who: Called by FriendsScreen on the Groups tab.
 * When: Rendered for every group in the list.
 */
@Composable
private fun GroupRow(group: GroupSummary, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Sage.copy(alpha = 0.7f))
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

/**
 * What: Compact A-Z jump bar floating on the right edge. ★ scrolls to the start
 *       of the friend list (i.e. the sticky tab bar itself, so the new view
 *       reads from the top of the alphabet); # jumps to the non-letter bucket
 *       when one exists. Unavailable letters render greyed and don't accept
 *       taps so the user isn't misled about what's reachable.
 * Who: Called by FriendsScreen on the FRIENDS tab.
 * When: Visible alongside the friend list (not on the Groups tab).
 */
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

/**
 * What: One letter/symbol in the alphabet jump bar. Tiny tap target keeps the
 *       full A-Z + ★ + # column inside a normal screen height.
 * Who: Called by AlphabetIndex.
 * When: Rendered for each entry of the index.
 */
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

/**
 * What: Fallback shown when the active tab has no rows (no friends, no groups).
 *       Keeps the page from collapsing to just the sticky tab bar.
 * Who: Called by FriendsScreen.
 * When: Rendered only when the active tab's data list is empty.
 */
@Composable
private fun EmptyHint(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp)
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyMedium,
            color = InkTertiary
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
            onOpenMemberProfile = {},
            onOpenGroupDetail   = {}
        )
    }
}
