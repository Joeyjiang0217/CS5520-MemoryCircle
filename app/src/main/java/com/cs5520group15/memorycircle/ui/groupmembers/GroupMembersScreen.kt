package com.cs5520group15.memorycircle.ui.groupmembers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.ui.common.AvatarListRow
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.common.SectionHeader
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
                    SectionHeader(
                        text = "MEMBERS",
                        trailing = {
                            Text(
                                text  = "${members.size} total",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Brown
                            )
                        }
                    )
                }
                items(members, key = { it.id }) { member ->
                    AvatarListRow(
                        name       = member.name,
                        subtitle   = "${member.sharedMemories} shared memories",
                        isOnline   = member.isOnline,
                        avatarSize = 48.dp,
                        onClick    = { onOpenMemberProfile(member.id) }
                    )
                    HorizontalDivider(color = Beige.copy(alpha = 0.5f))
                }
            }
        }
    }
}

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
