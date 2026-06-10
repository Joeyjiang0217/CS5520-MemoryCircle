package com.cs5520group15.memorycircle.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Placeholder for a group's members page, reached from the contacts icon in
 *       the timeline top bar. The real UI (member count, member list, roles, etc.)
 *       is owned by a teammate; this stub only exists so the icon has a destination
 *       and the app builds and runs.
 * Who: Called by MemoryCircleNavigation for the GroupMembers route.
 * When: Displayed when the user taps the contacts icon on the timeline screen.
 *
 * @param groupId the group whose members will be shown (passed through for the
 *                teammate's implementation; unused in this placeholder)
 */
@Composable
fun GroupMembersScreen(
    groupId: String,
    onBack:  () -> Unit
) {
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
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text      = "Group members",
                    style     = MaterialTheme.typography.titleLarge,
                    color     = Ink,
                    textAlign = TextAlign.Center
                )
                Text(
                    text      = "Coming soon — this is where you'll see how many people are in this group and who they are.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = InkTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GroupMembersScreenPreview() {
    MemoryCircleTheme {
        GroupMembersScreen(groupId = "1", onBack = {})
    }
}
