/**
 * What: Jetpack Compose UI for the Notification Settings screen — toggles for the
 *       app's notification preferences.
 * Who:  Wired into the nav graph by MemoryCircleNavigation; reached from SettingsScreen
 *       via its "open notification settings" action.
 * When: Composed when the user navigates to the NotificationSettings route from Settings.
 */

package com.cs5520group15.memorycircle.ui.notifications

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
import com.cs5520group15.memorycircle.data.NotificationSettingsRepository
import com.cs5520group15.memorycircle.model.NotificationSettings
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.common.RowDivider
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

    NotificationSettingsContent(
        settings                = settings,
        onBack                  = onBack,
        onToggleFriendRequests  = NotificationSettingsRepository::setNewFriendRequests,
        onToggleGroupActivity   = NotificationSettingsRepository::setNewGroupActivity,
        onToggleMemoryPosts     = NotificationSettingsRepository::setNewMemoryPosts
    )
}

/**
 * Stateless body — takes the settings struct + callbacks so it renders in
 * @Preview without touching the repository. NotificationSettingsScreen above
 * is the thin wrapper that wires the repository.
 */
@Composable
private fun NotificationSettingsContent(
    settings:               NotificationSettings,
    onBack:                 () -> Unit,
    onToggleFriendRequests: (Boolean) -> Unit,
    onToggleGroupActivity:  (Boolean) -> Unit,
    onToggleMemoryPosts:    (Boolean) -> Unit
) {
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
                onChange    = onToggleFriendRequests
            )
            RowDivider()

            ToggleRow(
                label       = "New group activity",
                description = "Push me when I'm added to a group or someone joins one of mine.",
                checked     = settings.newGroupActivity,
                onChange    = onToggleGroupActivity
            )
            RowDivider()

            ToggleRow(
                label       = "New memory posts",
                description = "Push me when a group member adds a new photo to a scrapbook.",
                checked     = settings.newMemoryPosts,
                onChange    = onToggleMemoryPosts
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
 * Who: Called by NotificationSettingsContent.
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

// ---------------------------------------------------------------------------
// Previews — each one targets the whole screen at a different UI state.
// ---------------------------------------------------------------------------

/** Default — all three channels enabled (factory state). */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Notifications · all on")
@Composable
fun NotificationSettingsScreenPreview() {
    MemoryCircleTheme {
        NotificationSettingsContent(
            settings               = NotificationSettings(),
            onBack                 = {},
            onToggleFriendRequests = {},
            onToggleGroupActivity  = {},
            onToggleMemoryPosts    = {}
        )
    }
}

/** All channels muted — every switch off. */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Notifications · all off")
@Composable
fun NotificationSettingsScreenAllOffPreview() {
    MemoryCircleTheme {
        NotificationSettingsContent(
            settings               = NotificationSettings(
                newFriendRequests = false,
                newGroupActivity  = false,
                newMemoryPosts    = false
            ),
            onBack                 = {},
            onToggleFriendRequests = {},
            onToggleGroupActivity  = {},
            onToggleMemoryPosts    = {}
        )
    }
}

/** Mixed — friend requests off, group activity off, memory posts on. */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Notifications · mixed")
@Composable
fun NotificationSettingsScreenMixedPreview() {
    MemoryCircleTheme {
        NotificationSettingsContent(
            settings               = NotificationSettings(
                newFriendRequests = false,
                newGroupActivity  = false,
                newMemoryPosts    = true
            ),
            onBack                 = {},
            onToggleFriendRequests = {},
            onToggleGroupActivity  = {},
            onToggleMemoryPosts    = {}
        )
    }
}
