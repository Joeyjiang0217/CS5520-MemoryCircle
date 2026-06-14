package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.theme.DeleteRed
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Full-width outlined button styled destructively — red border, red label,
 *       optional leading icon. Used for the Leave-group button on GroupDetail
 *       and the Log-out button at the foot of SettingsScreen. The visual is
 *       identical so destructive secondary actions read consistently.
 * Who: Called by any screen with a destructive secondary action.
 * When: Rendered as the final affordance on the page.
 */
@Composable
fun DestructiveOutlinedButton(
    label:    String,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
    iconRes:  Int? = null
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        border   = BorderStroke(1.dp, DeleteRed.copy(alpha = 0.6f))
    ) {
        if (iconRes != null) {
            Icon(
                painter            = painterResource(iconRes),
                contentDescription = null,
                tint               = DeleteRed
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge,
            color = DeleteRed
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun DestructiveOutlinedButtonPreview() {
    MemoryCircleTheme {
        DestructiveOutlinedButton(
            label   = "Leave group",
            iconRes = R.drawable.ic_leave,
            onClick = {}
        )
    }
}
