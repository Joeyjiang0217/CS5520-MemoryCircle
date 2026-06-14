package com.cs5520group15.memorycircle.ui.friendrequests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.model.FriendRequest
import com.cs5520group15.memorycircle.ui.common.AcceptPill
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.ConfirmDialog
import com.cs5520group15.memorycircle.ui.common.DeclineCircleButton
import com.cs5520group15.memorycircle.ui.common.EmptyHint
import com.cs5520group15.memorycircle.ui.common.LockedPill
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*

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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                EmptyHint(text = "No friend requests yet.")
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

    if (pendingDeleteRequest != null) {
        ConfirmDialog(
            title        = "Delete request?",
            message      = "${pendingDeleteRequest.fromUserName}'s friend request " +
                           "will be removed permanently.",
            confirmLabel = "Delete",
            onConfirm    = {
                viewModel.delete(pendingDeleteRequest.id)
                pendingDeleteId = null
            },
            onDismiss    = { pendingDeleteId = null }
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
                true
            } else false
        }
    )

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

            when (request.status) {
                FriendRequest.Status.PENDING -> {
                    AcceptPill(label = "Accept", onClick = onAccept)
                    Spacer(modifier = Modifier.width(6.dp))
                    DeclineCircleButton(onClick = onDecline)
                }
                FriendRequest.Status.ACCEPTED -> LockedPill(label = "✓ Accepted")
                FriendRequest.Status.DECLINED -> LockedPill(label = "✕ Declined")
            }
        }
    }
}

private fun subtitleFor(request: FriendRequest): String = when (request.status) {
    FriendRequest.Status.ACCEPTED -> "Accepted"
    FriendRequest.Status.DECLINED -> "Declined"
    FriendRequest.Status.PENDING  ->
        if (request.mutualFriends > 0)
            "${request.mutualFriends} mutual friend${if (request.mutualFriends == 1) "" else "s"}"
        else
            request.fromUserEmail
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun AllFriendRequestsScreenPreview() {
    MemoryCircleTheme {
        AllFriendRequestsScreen(onBack = {})
    }
}
