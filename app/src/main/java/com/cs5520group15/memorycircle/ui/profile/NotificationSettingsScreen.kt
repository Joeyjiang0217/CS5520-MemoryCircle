package com.cs5520group15.memorycircle.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Three-toggle notification settings page. Each row controls one push
 *       channel — friend requests, group activity, and new memory posts —
 *       toggled independently. All three default to on; the user can mute
 *       any channel without affecting the others.
 * Who: Called by MemoryCircleNavigation for the NotificationSettings route.
 * When: Reached from the Notifications row on SettingsScreen.
 */
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit
) {
    val settings by NotificationSettingsRepository.settings.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "Notifications",
                showBack = true,
                onBack   = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            ToggleRow(
                label       = "New friend requests",
                description = "Push me when someone sends a friend request.",
                checked     = settings.newFriendRequests,
                onChange    = NotificationSettingsRepository::setNewFriendRequests
            )
            RowDivider()

            ToggleRow(
                label       = "New group activity",
                description = "Push me when I'm added to a group or someone joins one of mine.",
                checked     = settings.newGroupActivity,
                onChange    = NotificationSettingsRepository::setNewGroupActivity
            )
            RowDivider()

            ToggleRow(
                label       = "New memory posts",
                description = "Push me when a group member adds a new photo to a scrapbook.",
                checked     = settings.newMemoryPosts,
                onChange    = NotificationSettingsRepository::setNewMemoryPosts
            )
            RowDivider()
        }
    }
}

/**
 * What: One labelled row with a Switch on the right. Carries a small
 *       descriptive caption beneath the label so the user knows exactly
 *       what's being silenced. Material3 Switch is themed Sage / Cream
 *       to match the brand palette.
 * Who: Called by NotificationSettingsScreen.
 * When: Rendered for each toggle.
 */
@Composable
private fun ToggleRow(
    label:       String,
    description: String,
    checked:     Boolean,
    onChange:    (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Ink
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = description,
                style = MaterialTheme.typography.bodyMedium,
                color = InkTertiary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked         = checked,
            onCheckedChange = onChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor    = Cream,
                checkedTrackColor    = AccentGreen,
                checkedBorderColor   = AccentGreen,
                uncheckedThumbColor  = Cream,
                uncheckedTrackColor  = BrownDisabled,
                uncheckedBorderColor = BrownDisabled
            )
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = Beige.copy(alpha = 0.5f))
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun NotificationSettingsScreenPreview() {
    MemoryCircleTheme {
        NotificationSettingsScreen(onBack = {})
    }
}
