package com.cs5520group15.memorycircle.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Flat, tappable list of everyone in a group — header with group name and
 *       member count, then one row per member. Each row opens that member's
 *       profile (route TBD). The "leave group" action lives on GroupDetail's
 *       top bar, not here.
 * Who: Called by MemoryCircleNavigation for the GroupMembers route, reached from
 *       the "View all members" link on GroupDetail.
 * When: Displayed when the user wants the full roster.
 *
 * @param groupId             the group whose roster is shown; drives ViewModel binding
 * @param onOpenMemberProfile navigates to a member's profile (placeholder route)
 */
@Composable
fun GroupMembersScreen(
    groupId:              String,
    onBack:               () -> Unit,
    onOpenMemberProfile:  (String) -> Unit,
    viewModel:            GroupMembersViewModel = viewModel()
) {
    LaunchedEffect(groupId) { viewModel.bind(groupId) }

    val groupName by viewModel.groupName.collectAsStateWithLifecycle()
    val members   by viewModel.members.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "Members",
                showBack = true,
                onBack   = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding      = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { GroupHeader(groupName = groupName, memberCount = members.size) }

            if (members.isNotEmpty()) {
                item {
                    SectionLabel(
                        text     = "MEMBERS",
                        trailing = "${members.size} total"
                    )
                }
                items(members, key = { it.id }) { member ->
                    MemberRow(
                        member  = member,
                        onClick = { onOpenMemberProfile(member.id) }
                    )
                    HorizontalDivider(color = Beige.copy(alpha = 0.5f))
                }
            }
        }
    }
}

/**
 * What: Top header for the members screen — large serif group name and a one-line
 *       member-count subtitle, matching the "Friends & Groups" landing style.
 * Who: Called by GroupMembersScreen.
 * When: Rendered once above the list.
 */
@Composable
private fun GroupHeader(groupName: String, memberCount: Int) {
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
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * What: Small uppercase section heading (MEMBERS) with an optional trailing
 *       counter on the right — mirrors the section style used on Home/Friends.
 * Who: Called by GroupMembersScreen.
 * When: Rendered above the member rows.
 */
@Composable
private fun SectionLabel(text: String, trailing: String? = null) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.labelSmall,
            color = Ink
        )
        if (trailing != null) {
            Text(
                text  = trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = Brown
            )
        }
    }
}

/**
 * What: One tappable row in the member list — avatar (with optional online dot),
 *       name, and "N shared memories" sub-text. Tapping opens the member's
 *       profile.
 * Who: Called by GroupMembersScreen for every member.
 * When: Rendered for every entry in the members section.
 */
@Composable
private fun MemberRow(
    member:  Member,
    onClick: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AvatarCircle(name = member.name, size = 48.dp)
            if (member.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = member.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Ink
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = "${member.sharedMemories} shared memories",
                style = MaterialTheme.typography.bodyMedium,
                color = InkTertiary
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun GroupMembersScreenPreview() {
    MemoryCircleTheme {
        GroupMembersScreen(
            groupId             = "1",
            onBack              = {},
            onOpenMemberProfile = {}
        )
    }
}
