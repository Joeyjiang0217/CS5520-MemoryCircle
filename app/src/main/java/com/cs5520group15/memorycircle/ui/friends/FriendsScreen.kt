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
// SwipeToDismissBox lives in material3 (re-exported by the wildcard above),
// but the threshold helpers + value enum need explicit imports.
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.data.NetworkUtil
import com.cs5520group15.memorycircle.model.Friend
import com.cs5520group15.memorycircle.model.FriendRequest
import com.cs5520group15.memorycircle.model.GroupSummary
import com.cs5520group15.memorycircle.ui.common.AcceptPill
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.AvatarListRow
import com.cs5520group15.memorycircle.ui.common.ConfirmDialog
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

    var selectedTab    by rememberSaveable { mutableStateOf(ContactsTab.FRIENDS) }
    var pendingDeleteId by remember        { mutableStateOf<String?>(null) }

    val listState         = rememberLazyListState()
    val scope             = rememberCoroutineScope()
    val context           = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val tabBarHeightPx = with(LocalDensity.current) { 54.dp.roundToPx() }

    // Requests section is always rendered (so "See all" stays reachable
    // even with zero pending), so the sticky tab row is always at index 3.
    val tabBarIndex = 3

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
        snackbarHost   = { SnackbarHost(snackbarHostState) },
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
                                SwipeableFriendRow(
                                    friend          = friend,
                                    pendingDeleteId = pendingDeleteId,
                                    onClick         = { onOpenMemberProfile(friend.id) },
                                    onSwipedAway    = { pendingDeleteId = friend.id }
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
                                name             = group.name,
                                memberCount      = group.memberCount,
                                memberAvatarUrls = group.memberAvatarUrls,
                                memberNames      = group.memberNames,
                                onClick          = { onOpenGroupDetail(group.id) },
                                modifier         = Modifier.padding(horizontal = 24.dp)
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

    val pendingFriend = pendingDeleteId?.let { id -> friends.firstOrNull { it.id == id } }
    if (pendingFriend != null) {
        ConfirmDialog(
            title        = "Remove friend?",
            message      = "Remove ${pendingFriend.name.ifBlank { "this user" }} from your friend list? " +
                           "They'll also stop seeing you as a friend.",
            confirmLabel = "Remove",
            confirmColor = DeleteRed,
            onConfirm    = {
                // Clear pendingDeleteId first so the SwipeToDismissBox snaps
                // back via the existing LaunchedEffect, regardless of whether
                // we actually issue the delete. Network gate matches the
                // GroupDetail destructive ops: offline writes would otherwise
                // silently queue into Firestore's offline cache and apply
                // later — which is exactly the bug we just fixed there.
                val targetId = pendingFriend.id
                pendingDeleteId = null
                if (NetworkUtil.isOnline(context)) {
                    viewModel.deleteFriend(targetId)
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("No internet connection. Please try again when online.")
                    }
                }
            },
            onDismiss    = { pendingDeleteId = null }
        )
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

/**
 * What: The "FRIEND REQUESTS (N)" block on the Friends landing tab. Always
 *       renders the header + "See all ›" so the user can reach
 *       AllFriendRequestsScreen (and inspect their actioned history) even
 *       when no PENDING requests exist. The latest-pending preview card
 *       only renders when there's something to act on; the count `N`
 *       always tracks the PENDING total (actioned history doesn't inflate
 *       it).
 * Who: Called from FriendsScreen's LazyColumn.
 * When: Every recomposition of the Friends tab.
 */
@Composable
private fun FriendRequestsSection(
    pending:   List<FriendRequest>,
    onAccept:  (String) -> Unit,
    onReject:  (String) -> Unit,
    onOpenAll: () -> Unit,
    modifier:  Modifier = Modifier
) {
    val newest = pending.firstOrNull()

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

        if (newest != null) {
            FriendRequestCard(
                request  = newest,
                onAccept = { onAccept(newest.id) },
                onReject = { onReject(newest.id) }
            )
        }
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
                    text  = requestSubtitle(request),
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

/**
 * Subtitle fallback for a friend-request row. Mutual-friend count wins when
 * we have one (currently always 0 — would require a friends ∩ friends scan
 * we don't run yet); otherwise the sender's bio if non-empty; else a fixed
 * "Wants to be your friend" line so the row never looks half-rendered.
 */
private fun requestSubtitle(request: FriendRequest): String = when {
    request.mutualFriends > 0 ->
        "${request.mutualFriends} mutual friend${if (request.mutualFriends == 1) "" else "s"}"
    request.fromUserBio.isNotBlank() -> request.fromUserBio
    else                             -> "Wants to be your friend"
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

/**
 * What: One swipeable friend row. Drag-from-end commits the SwipeToDismissBox
 *       to the EndToStart anchor and fires onSwipedAway, exposing the red
 *       background. The parent owns the confirmation dialog; when the dialog
 *       is dismissed (cancel path) the LaunchedEffect resets the swipe so the
 *       row snaps back. Confirm path triggers the real delete and the row
 *       leaves the list via the Firestore listener.
 *
 *       Subtitle is intentionally blank when sharedMemories == 0 — showing
 *       "0 shared memories" on a brand-new friend reads as noise (the user
 *       saw nothing meaningful to count yet).
 * Who: Called by FriendsScreen.
 * When: Rendered for every friend on the Friends tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableFriendRow(
    friend:          Friend,
    pendingDeleteId: String?,
    onClick:         () -> Unit,
    onSwipedAway:    () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipedAway()
                true
            } else false
        }
    )

    // The reset has two timelines:
    //   - explicit dialog cancel / confirm → animate (smooth snap-back)
    //   - row entering composition with a stale dismiss value preserved by
    //     the LazyColumn / nav back stack → snap silently so the user doesn't
    //     see a phantom animation play on their way back into the screen.
    //
    // `hasInteracted` flips on the first observed transition INTO this row's
    // pending-delete slot, so cancellation-driven resets after that point
    // keep their animation.
    var hasInteracted by remember { mutableStateOf(false) }
    LaunchedEffect(pendingDeleteId) {
        val thisRowIsActive = pendingDeleteId == friend.id
        if (thisRowIsActive) hasInteracted = true
        if (!thisRowIsActive && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            if (hasInteracted) {
                dismissState.reset()
            } else {
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
        }
    }

    SwipeToDismissBox(
        state                       = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent           = { FriendSwipeBackground() }
    ) {
        Box(modifier = Modifier.background(Cream)) {
            AvatarListRow(
                name     = friend.name,
                subtitle = friend.bio,
                isOnline = friend.isOnline,
                photoUrl = friend.avatarUrl,
                onClick  = onClick,
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
}

@Composable
private fun FriendSwipeBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeleteRed)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            painter            = painterResource(R.drawable.ic_delete),
            contentDescription = "Remove friend",
            tint               = Cream,
            modifier           = Modifier.size(24.dp)
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
