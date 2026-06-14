package com.cs5520group15.memorycircle.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * Muted warm red used for the destructive Logout label. Same hex as the
 * other destructive surfaces in the app (DeleteRed on AllFriendRequests and
 * GroupDetail). Kept local until the brand palette adopts a danger token.
 */
private val DeleteRed = Color(0xFFC25B5B)

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

            // Logout sits at the bottom as an outlined button (mirrors the
            // "Leave group" button on GroupDetail) so it reads as a deliberate
            // exit affordance rather than another navigation row.
            LogoutButton(onClick = { showLogoutDialog = true })

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

/**
 * What: Confirmation dialog shown before the user actually logs out. Mirrors
 *       screenshot 3's wording so the user knows their history sticks around
 *       and that the same account is reusable on next sign-in. "Log out" is
 *       tinted destructive-red, "Cancel" stays AccentGreen.
 * Who: Called by SettingsScreen when the user taps the Log out row.
 * When: While showLogoutDialog is true.
 */
@Composable
private fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Cream,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Text(
                text  = "Log out?",
                style = MaterialTheme.typography.titleLarge,
                color = Ink
            )
        },
        text = {
            Text(
                text  = "Logging out won't delete any of your history — " +
                        "you can sign back in with this account any time.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text  = "Log out",
                    style = MaterialTheme.typography.labelLarge,
                    color = DeleteRed
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text  = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentGreen
                )
            }
        }
    )
}

/**
 * What: One tappable settings row — label on the left, chevron on the right.
 *       Plain navigation row; the destructive Log out treatment lives in
 *       LogoutButton, not here.
 * Who: Called by SettingsScreen.
 * When: Rendered for every navigation entry on the settings hub.
 */
@Composable
private fun SettingsRow(
    label:   String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Ink,
            modifier = Modifier.weight(1f)
        )
        Text(
            text  = "›",
            style = MaterialTheme.typography.titleLarge,
            color = InkTertiary
        )
    }
}

/**
 * What: Outlined Log out button rendered at the bottom of the settings hub.
 *       Same shape, border weight, and icon-plus-label layout as the Leave
 *       group button on GroupDetail — destructive red border, ic_leave icon,
 *       red label. Tapping it surfaces LogoutConfirmDialog rather than
 *       logging the user out directly.
 * Who: Called by SettingsScreen.
 * When: Rendered once at the foot of the page.
 */
@Composable
private fun LogoutButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        border   = BorderStroke(1.dp, DeleteRed.copy(alpha = 0.6f))
    ) {
        Icon(
            painter            = painterResource(R.drawable.ic_leave),
            contentDescription = null,
            tint               = DeleteRed
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text  = "Log out",
            style = MaterialTheme.typography.labelLarge,
            color = DeleteRed
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = Beige.copy(alpha = 0.5f))
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
