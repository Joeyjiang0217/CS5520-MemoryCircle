package com.cs5520group15.memorycircle.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.common.ConfirmDialog
import com.cs5520group15.memorycircle.ui.common.DestructiveOutlinedButton
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.common.RowDivider
import com.cs5520group15.memorycircle.ui.common.SettingsRow
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Trimmed-down settings hub reached from the "Settings" row on
 *       EditProfile. Only three entries per spec — Profile, Notifications,
 *       Log out — so the user has a single, simple surface to manage their
 *       account without WeChat-style depth.
 * Who: Called by MemoryCircleNavigation for the Settings route.
 * When: Reached from the Settings row on EditProfileScreen.
 *
 * @param onOpenProfile              navigates to the EditProfile screen
 *                                   (same destination as the Edit Profile
 *                                   button on ProfileScreen)
 * @param onOpenNotificationSettings navigates to the notifications page
 * @param onLogout                   triggers the logout flow (UI for the
 *                                   post-logout state is owned by a later
 *                                   change; for now this is a no-op hook)
 */
@Composable
fun SettingsScreen(
    onBack:                      () -> Unit,
    onOpenProfile:               () -> Unit,
    onOpenNotificationSettings:  () -> Unit,
    onLogout:                    () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "Settings",
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
            SettingsRow(label = "Profile",       onClick = onOpenProfile)
            RowDivider()
            SettingsRow(label = "Notifications", onClick = onOpenNotificationSettings)
            RowDivider()

            Spacer(modifier = Modifier.weight(1f))

            DestructiveOutlinedButton(
                label   = "Log out",
                iconRes = R.drawable.ic_leave,
                onClick = { showLogoutDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showLogoutDialog) {
        ConfirmDialog(
            title        = "Log out?",
            message      = "Logging out won't delete any of your history — " +
                           "you can sign back in with this account any time.",
            confirmLabel = "Log out",
            onConfirm    = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss    = { showLogoutDialog = false }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun SettingsScreenPreview() {
    MemoryCircleTheme {
        SettingsScreen(
            onBack                     = {},
            onOpenProfile              = {},
            onOpenNotificationSettings = {},
            onLogout                   = {}
        )
    }
}
