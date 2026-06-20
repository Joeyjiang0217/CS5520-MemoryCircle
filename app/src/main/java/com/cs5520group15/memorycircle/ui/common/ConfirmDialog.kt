/**
 * What: Branded confirmation modal with a destructive-tinted confirm button.
 * Who:  Used by SettingsScreen (Logout), GroupDetailScreen (Leave), AllFriendRequestsScreen,
 *       and FriendsScreen.
 * When: Composed while the parent screen's confirmation-dialog state is true.
 */

package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.ui.theme.AccentGreen
import com.cs5520group15.memorycircle.ui.theme.Cream
import com.cs5520group15.memorycircle.ui.theme.DeleteRed
import com.cs5520group15.memorycircle.ui.theme.Ink
import com.cs5520group15.memorycircle.ui.theme.InkSecondary
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Branded confirmation dialog — Cream container, 20dp rounded shape,
 *       titleLarge title, bodyMedium body, AccentGreen Cancel, destructive-tinted
 *       confirm button. Centralises the visual treatment that previously lived
 *       inline on GroupDetail's Leave dialog, Settings' Logout dialog, and
 *       AllFriendRequests' Delete dialog.
 * Who: Called by any screen needing a destructive confirmation modal.
 * When: While the parent's `show*Dialog` state is true.
 */
@Composable
fun ConfirmDialog(
    title:        String,
    message:      String,
    confirmLabel: String,
    onConfirm:    () -> Unit,
    onDismiss:    () -> Unit,
    confirmColor: Color = DeleteRed,
    cancelLabel:  String = "Cancel"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Cream,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Text(
                text  = title,
                style = MaterialTheme.typography.titleLarge,
                color = Ink
            )
        },
        text = {
            Text(
                text  = message,
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text  = confirmLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = confirmColor
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text  = cancelLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentGreen
                )
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun ConfirmDialogPreview() {
    MemoryCircleTheme {
        ConfirmDialog(
            title        = "Log out?",
            message      = "You can sign back in anytime.",
            confirmLabel = "Log out",
            onConfirm    = {},
            onDismiss    = {}
        )
    }
}
