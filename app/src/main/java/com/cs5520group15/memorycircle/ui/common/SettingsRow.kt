package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.theme.Beige
import com.cs5520group15.memorycircle.ui.theme.Brown
import com.cs5520group15.memorycircle.ui.theme.Ink
import com.cs5520group15.memorycircle.ui.theme.InkSecondary
import com.cs5520group15.memorycircle.ui.theme.InkTertiary
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: One tappable settings / edit-profile row — bold label on the left, an
 *       optional value (right-aligned, ellipsised) in the middle, chevron on
 *       the right. Optional leading icon-in-rounded-square for the entries
 *       that need it (the Settings row on ProfileScreen). Replaces the three
 *       near-identical SettingsEntryRow / SettingsRow / FieldRow helpers
 *       across Profile, Settings, and EditProfile.
 * Who: Called by ProfileScreen, SettingsScreen, and EditProfileScreen.
 * When: Rendered for each row of those forms.
 */
@Composable
fun SettingsRow(
    label:           String,
    onClick:         () -> Unit,
    modifier:        Modifier = Modifier,
    value:           String? = null,
    valuePlaceholder: Boolean = false,
    leadingIconRes:  Int? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp)
    ) {
        if (leadingIconRes != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Beige.copy(alpha = 0.4f))
            ) {
                Icon(
                    painter            = painterResource(leadingIconRes),
                    contentDescription = null,
                    tint               = Brown,
                    modifier           = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Ink,
            modifier = if (value == null) Modifier.weight(1f) else Modifier
        )
        if (value != null) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text     = value,
                style    = MaterialTheme.typography.bodyLarge,
                color    = if (valuePlaceholder) InkTertiary else InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Chevron()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun SettingsRowPreview() {
    MemoryCircleTheme {
        SettingsRow(
            label          = "Settings",
            leadingIconRes = R.drawable.ic_setting,
            onClick        = {}
        )
    }
}
