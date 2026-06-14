package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.ui.theme.BrownDisabled
import com.cs5520group15.memorycircle.ui.theme.InkSecondary
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Non-interactive locked pill used for terminal states — "Invitation sent",
 *       "Added", "✓ Accepted", "✕ Declined". BrownDisabled bg + InkSecondary
 *       label so the row reads as past-tense / unavailable everywhere it appears.
 * Who: Called by friend-request rows and add-friend search rows.
 * When: Rendered when the corresponding action is no longer available.
 */
@Composable
fun LockedPill(
    label:    String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BrownDisabled)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge,
            color = InkSecondary
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun LockedPillPreview() {
    MemoryCircleTheme {
        LockedPill(label = "Invitation sent")
    }
}
