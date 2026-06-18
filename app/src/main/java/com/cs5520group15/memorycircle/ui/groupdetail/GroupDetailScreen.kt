package com.cs5520group15.memorycircle.ui.groupdetail

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.data.AuthRepository
import com.cs5520group15.memorycircle.model.Member
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.ConfirmDialog
import com.cs5520group15.memorycircle.ui.common.DestructiveOutlinedButton
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.common.MonthScrapbookRow
import com.cs5520group15.memorycircle.ui.common.SectionHeader
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: A group's "settings/details" page reached from the menu icon on the
 *       timeline top bar. Three stacked sections — group name hero, members
 *       thumbnail card (with "View all members" and an invite "+" tile), and a
 *       per-month scrapbook list for this group. The top bar carries a
 *       confirmation-gated "leave group" action on the right.
 * Who: Called by MemoryCircleNavigation for the GroupDetail route.
 * When: Displayed when the user taps the menu icon on the timeline screen.
 *
 * @param groupId              the group whose details are shown
 * @param onOpenAllMembers     navigates to the flat members list
 * @param onOpenMemberProfile  navigates to a member's profile (placeholder route)
 * @param onInviteMember       opens the invite-friend flow (placeholder route)
 * @param onOpenScrapbook      opens a month's scrapbook viewer
 */
@Composable
fun GroupDetailScreen(
    groupId:              String,
    onBack:               () -> Unit,
    onOpenAllMembers:     () -> Unit,
    onOpenMemberProfile:  (String) -> Unit,
    onInviteMember:       () -> Unit,
    onOpenScrapbook:      (groupId: String, month: String, year: String) -> Unit,
    /** Invoked after the user successfully leaves the group. The nav layer
     *  should navigate to Home and pop the back stack so the user does NOT
     *  fall back into the ScrapbookViewer of the group they just left. */
    onLeftGroup:          () -> Unit = onBack,
    viewModel:            GroupDetailViewModel = viewModel()
) {
    LaunchedEffect(groupId) { viewModel.bind(groupId) }

    val groupName by viewModel.groupName.collectAsStateWithLifecycle()
    val members   by viewModel.members.collectAsStateWithLifecycle()
    val months    by viewModel.months.collectAsStateWithLifecycle()
    val isOwner   by viewModel.isOwner.collectAsStateWithLifecycle()
    val currentUid = remember { AuthRepository.currentUid }

    var showLeaveDialog       by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var memberToKick          by remember { mutableStateOf<Member?>(null) }

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "",
                showBack = true,
                onBack   = onBack,
                actions  = {
                    IconButton(onClick = { showLeaveDialog = true }) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_leave),
                            contentDescription = "Leave group",
                            tint               = DeleteRed
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding      = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                GroupNameHero(groupName = groupName, memberCount = members.size)
            }

            item {
                MembersCard(
                    members             = members,
                    isOwner             = isOwner,
                    currentUid          = currentUid,
                    onMemberClick       = onOpenMemberProfile,
                    onSeeAllClick       = onOpenAllMembers,
                    onInviteClick       = onInviteMember,
                    onKickMember        = { member -> memberToKick = member }
                )
            }

            item {
                DestructiveOutlinedButton(
                    label   = "Leave group",
                    iconRes = R.drawable.ic_leave,
                    onClick = { showLeaveDialog = true }
                )
            }

            // Owner-only: delete the entire group. Shown as a second
            // destructive button below Leave so the order escalates in
            // severity (leave < delete).
            if (isOwner) {
                item {
                    DestructiveOutlinedButton(
                        label   = "Delete group",
                        iconRes = R.drawable.ic_leave,
                        onClick = { showDeleteGroupDialog = true }
                    )
                }
            }

            item {
                Text(
                    text  = "SCRAPBOOKS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink
                )
            }

            items(months, key = { it.id }) { month ->
                MonthScrapbookRow(
                    title       = "${month.month} ${month.year}",
                    memoryCount = month.memoryCount,
                    colorType   = month.colorType,
                    onClick     = { onOpenScrapbook(groupId, month.month, month.year) }
                )
            }
        }
    }

    if (showLeaveDialog) {
        ConfirmDialog(
            title        = "Leave group?",
            message      = "You'll stop receiving new memories from " +
                           "${groupName.ifBlank { "this group" }}. " +
                           "Your past photos and comments will stay.",
            confirmLabel = "Yes, leave",
            confirmColor = Brown,
            onConfirm    = {
                showLeaveDialog = false
                viewModel.leaveGroup(onDone = onLeftGroup)
            },
            onDismiss    = { showLeaveDialog = false }
        )
    }

    // Owner-only: delete the whole group (subcollections become orphans).
    if (showDeleteGroupDialog) {
        ConfirmDialog(
            title        = "Delete group?",
            message      = "${groupName.ifBlank { "This group" }} and its memories will be removed for every member. This can't be undone.",
            confirmLabel = "Delete forever",
            confirmColor = DeleteRed,
            onConfirm    = {
                showDeleteGroupDialog = false
                viewModel.deleteGroup(onDone = onLeftGroup)
            },
            onDismiss    = { showDeleteGroupDialog = false }
        )
    }

    // Owner-only: confirm kicking a specific member.
    memberToKick?.let { member ->
        ConfirmDialog(
            title        = "Remove ${member.name}?",
            message      = "${member.name} will lose access to this group's memories.",
            confirmLabel = "Remove",
            confirmColor = DeleteRed,
            onConfirm    = {
                viewModel.kickMember(member.id)
                memberToKick = null
            },
            onDismiss    = { memberToKick = null }
        )
    }
}

@Composable
private fun GroupNameHero(groupName: String, memberCount: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text  = groupName.ifBlank { "Group" },
            style = MaterialTheme.typography.headlineMedium,
            color = Ink
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text  = "$memberCount ${if (memberCount == 1) "member" else "members"}",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
    }
}

@Composable
private fun MembersCard(
    members:       List<Member>,
    isOwner:       Boolean,
    currentUid:    String?,
    onMemberClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    onInviteClick: () -> Unit,
    onKickMember:  (Member) -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = WhiteCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            SectionHeader(
                text = "MEMBERS",
                trailing = {
                    TextButton(
                        onClick        = onSeeAllClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text  = "View all ${members.size} members  ›",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Brown
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ThumbnailGrid(
                members       = members,
                isOwner       = isOwner,
                currentUid    = currentUid,
                onMemberClick = onMemberClick,
                onInviteClick = onInviteClick,
                onKickMember  = onKickMember
            )
        }
    }
}

@Composable
private fun ThumbnailGrid(
    members:       List<Member>,
    isOwner:       Boolean,
    currentUid:    String?,
    onMemberClick: (String) -> Unit,
    onInviteClick: () -> Unit,
    onKickMember:  (Member) -> Unit
) {
    val columns = 5
    val slots: List<Member?> = members + listOf<Member?>(null)
    val rows = slots.chunked(columns)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { slot ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (slot != null) {
                            // Owner sees a kick × overlay on every member
                            // that isn't themselves.
                            val showKick = isOwner && slot.id != currentUid
                            MemberThumbnail(
                                member       = slot,
                                onClick      = { onMemberClick(slot.id) },
                                showKick     = showKick,
                                onKickClick  = { onKickMember(slot) }
                            )
                        } else {
                            InviteThumbnail(onClick = onInviteClick)
                        }
                    }
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MemberThumbnail(
    member:      Member,
    onClick:     () -> Unit,
    showKick:    Boolean = false,
    onKickClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box {
            AvatarCircle(name = member.name, size = 48.dp)
            if (member.isOnline) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
            }
            // Owner-only: small × badge in the corner. Tapping it triggers
            // a kick-confirm dialog upstream. Sits over the avatar's top-end
            // so it doesn't crowd the online dot at the bottom-end.
            if (showKick) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(DeleteRed)
                        .clickable { onKickClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "×",
                        color = WhiteCard,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text       = member.name,
            style      = MaterialTheme.typography.bodyMedium,
            color      = Ink,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun InviteThumbnail(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Beige.copy(alpha = 0.4f))
                .border(1.dp, Brown.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                painter            = painterResource(R.drawable.ic_add),
                contentDescription = "Invite member",
                tint               = Brown
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text       = "Invite",
            style      = MaterialTheme.typography.bodyMedium,
            color      = InkSecondary,
            maxLines   = 1,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun GroupDetailScreenPreview() {
    MemoryCircleTheme {
        GroupDetailScreen(
            groupId             = "1",
            onBack              = {},
            onOpenAllMembers    = {},
            onOpenMemberProfile = {},
            onInviteMember      = {},
            onOpenScrapbook     = { _, _, _ -> }
        )
    }
}
