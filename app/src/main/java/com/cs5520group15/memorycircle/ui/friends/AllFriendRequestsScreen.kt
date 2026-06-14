package com.cs5520group15.memorycircle.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * Muted warm red for the destructive swipe background. Stays local until the
 * brand palette formally adopts a danger color.
 */
private val DeleteRed = Color(0xFFC25B5B)

/**
 * What: Full list of every friend request the current user has ever received.
 *       Rows STAY after being actioned so the user can see their history —
 *       accepted entries hide the × and lock the Accept pill as "✓ Accepted";
 *       declined entries hide the Accept button and draw a strikethrough
 *       across the whole card. Any row (pending / accepted / declined) can be
 *       left-swiped to surface a delete-confirmation dialog; only confirmed
 *       deletion actually removes the row, and deletion NEVER touches the
 *       friends list — see AllFriendRequestsViewModel.delete.
 * Who: Called by MemoryCircleNavigation for the AllFriendRequests route.
 * When: Displayed when the user taps "See all" on the Friends tab.
 */
@Composable
fun AllFriendRequestsScreen(
    onBack:    () -> Unit,
    viewModel: AllFriendRequestsViewModel = viewModel()
) {
    val requests by viewModel.requests.collectAsStateWithLifecycle()

    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    val pendingDeleteRequest = pendingDeleteId?.let { id -> requests.firstOrNull { it.id == id } }

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "Friend Requests",
                showBack = true,
                onBack   = onBack
            )
        }
    ) { padding ->
        if (requests.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text  = "No friend requests yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTertiary
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(requests, key = { it.id }) { request ->
                SwipeableRequestRow(
                    request         = request,
                    pendingDeleteId = pendingDeleteId,
                    onAccept        = { viewModel.accept(request.id) },
                    onDecline       = { viewModel.decline(request.id) },
                    onSwipedAway    = { pendingDeleteId = request.id }
                )
            }
        }
    }

    // Delete-confirmation dialog. The corresponding row stays in its swiped-off
    // state while this is on screen — SwipeableRequestRow's LaunchedEffect
    // springs the row back when pendingDeleteId clears without a confirm.
    if (pendingDeleteRequest != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            containerColor   = Cream,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text  = "Delete request?",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink
                )
            },
            text = {
                Text(
                    text  = "${pendingDeleteRequest.fromUserName}'s friend request " +
                            "will be removed permanently.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(pendingDeleteRequest.id)
                    pendingDeleteId = null
                }) {
                    Text(
                        text  = "Delete",
                        style = MaterialTheme.typography.labelLarge,
                        color = DeleteRed
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(
                        text  = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        color = AccentGreen
                    )
                }
            }
        )
    }
}

/**
 * What: One swipeable request row. Every status accepts a right-to-left drag —
 *       confirmValueChange returns true so the SwipeToDismissBox commits to the
 *       EndToStart position, leaving the red delete background fully visible
 *       in the row's place while the confirmation dialog is on screen. The
 *       LaunchedEffect snaps the row back to its resting position the moment
 *       pendingDeleteId no longer points at this request (cancel path).
 * Who: Called by AllFriendRequestsScreen for every request.
 * When: Rendered for every row in the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRequestRow(
    request:         FriendRequest,
    pendingDeleteId: String?,
    onAccept:        () -> Unit,
    onDecline:       () -> Unit,
    onSwipedAway:    () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipedAway()
                true  // commit to EndToStart → row stays off-screen until reset / removal
            } else false
        }
    )

    // Cancel-path reset: when the dialog closes without confirming, the parent
    // sets pendingDeleteId back to null. That changes this LaunchedEffect's key
    // and we slide the row back to its resting position.
    LaunchedEffect(pendingDeleteId) {
        val thisRowIsActive = pendingDeleteId == request.id
        if (!thisRowIsActive && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state                       = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent           = { SwipeBackground() }
    ) {
        RequestRow(
            request   = request,
            onAccept  = onAccept,
            onDecline = onDecline
        )
    }
}

/**
 * What: The destructive red background revealed behind a row when it is
 *       swiped leftward. Right-aligned trash icon — gesture intent reads
 *       clearly without copy.
 * Who: Called by SwipeableRequestRow as the swipe backdrop.
 * When: Continuously rendered behind the SwipeToDismissBox content; only
 *       visible while the row is off its resting position.
 */
@Composable
private fun SwipeBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(DeleteRed)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            painter            = painterResource(R.drawable.ic_delete),
            contentDescription = "Delete",
            tint               = Cream,
            modifier           = Modifier.size(24.dp)
        )
    }
}

/**
 * What: The actual visual row — avatar, name + sub-line, and per-status action
 *       buttons. Wrapped in an opaque Cream Box so the swipe background never
 *       bleeds through at rest.
 *       Visual treatment varies by request.status:
 *         PENDING  → Accept (AccentGreen) + × — both interactive
 *         ACCEPTED → "✓ Accepted" locked pill, × hidden entirely
 *         DECLINED → "✕ Declined" locked pill, Accept hidden entirely
 * Who: Called by SwipeableRequestRow.
 * When: Rendered as the foreground of each swipeable row.
 */
@Composable
private fun RequestRow(
    request:   FriendRequest,
    onAccept:  () -> Unit,
    onDecline: () -> Unit
) {
    val actioned = request.status != FriendRequest.Status.PENDING

    // Opaque base — Cream covers the swipe background so the inner tint
    // composites over Cream, matching how FriendsScreen's request card looks.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Cream)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (actioned) Beige.copy(alpha = 0.25f)
                    else          Sage.copy(alpha = 0.18f)
                )
                .border(
                    width = 1.dp,
                    color = if (actioned) Beige.copy(alpha = 0.6f)
                            else          Sage.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            AvatarCircle(name = request.fromUserName, size = 44.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = request.fromUserName,
                    style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color    = if (actioned) InkSecondary else Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text     = subtitleFor(request),
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = InkTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Buttons depend on status — once a decision is made the irrelevant
            // button is removed entirely so it can't mislead the user.
            when (request.status) {
                FriendRequest.Status.PENDING -> {
                    AcceptButton(onClick = onAccept)
                    Spacer(modifier = Modifier.width(6.dp))
                    DeclineButton(onClick = onDecline)
                }
                FriendRequest.Status.ACCEPTED -> StatusPill(label = "✓ Accepted")
                FriendRequest.Status.DECLINED -> StatusPill(label = "✕ Declined")
            }
        }
    }
}

/**
 * What: Builds the secondary line under the requester's name. Falls back to
 *       a status hint ("Accepted" / "Declined") once the user has actioned the
 *       request so they always see what state the row is in.
 * Who: Called by RequestRow.
 * When: Per recomposition of a row.
 */
private fun subtitleFor(request: FriendRequest): String = when (request.status) {
    FriendRequest.Status.ACCEPTED -> "Accepted"
    FriendRequest.Status.DECLINED -> "Declined"
    FriendRequest.Status.PENDING  ->
        if (request.mutualFriends > 0)
            "${request.mutualFriends} mutual friend${if (request.mutualFriends == 1) "" else "s"}"
        else
            request.fromUserEmail
}

/**
 * What: Interactive Accept pill — AccentGreen background, Cream text. Only
 *       rendered for PENDING rows; the locked "✓ Accepted" / "× Declined"
 *       chips that mark actioned rows are handled by StatusPill instead.
 * Who: Called by RequestRow.
 * When: Rendered for PENDING rows only.
 */
@Composable
private fun AcceptButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AccentGreen)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text  = "Accept",
            style = MaterialTheme.typography.labelLarge,
            color = Cream
        )
    }
}

/**
 * What: Interactive Decline ("×") pill — round neutral button. Only rendered
 *       for PENDING rows; actioned rows use StatusPill instead.
 * Who: Called by RequestRow.
 * When: Rendered for PENDING rows only.
 */
@Composable
private fun DeclineButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(GraySoft)
            .clickable { onClick() }
    ) {
        Text(
            text  = "✕",
            style = MaterialTheme.typography.labelLarge,
            color = InkSecondary
        )
    }
}

/**
 * What: Locked status pill displayed in place of the action buttons once a
 *       request has been actioned. Same shape and tone for ACCEPTED ("✓ Accepted")
 *       and DECLINED ("× Declined") — the label is the only thing that changes.
 *       Non-interactive: the decision can't be reversed by tapping the pill.
 * Who: Called by RequestRow for ACCEPTED and DECLINED rows.
 * When: Rendered once per actioned row.
 */
@Composable
private fun StatusPill(label: String) {
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

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun AllFriendRequestsScreenPreview() {
    MemoryCircleTheme {
        AllFriendRequestsScreen(onBack = {})
    }
}
