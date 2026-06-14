package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.ui.theme.Brown
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Full-width outlined button with a Brown border and label — used for
 *       secondary affordances (Edit Profile, Add my photo). 20dp rounded shape
 *       matches the Leave-group / Log-out outline so the screen reads with one
 *       consistent button family.
 * Who: Called by ProfileScreen, ScrapbookViewerScreen, and any other surface
 *      needing a non-destructive secondary CTA.
 * When: Rendered as a secondary action below the primary or main content.
 */
@Composable
fun SecondaryOutlinedButton(
    label:       String,
    onClick:     () -> Unit,
    modifier:    Modifier = Modifier,
    borderColor: Color = Brown.copy(alpha = 0.5f),
    contentColor: Color = Brown
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        border   = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun SecondaryOutlinedButtonPreview() {
    MemoryCircleTheme {
        SecondaryOutlinedButton(label = "Edit Profile", onClick = {})
    }
}
