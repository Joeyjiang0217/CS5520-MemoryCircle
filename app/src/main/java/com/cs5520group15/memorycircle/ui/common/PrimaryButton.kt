package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.ui.theme.BrownDisabled
import com.cs5520group15.memorycircle.ui.theme.Cream
import com.cs5520group15.memorycircle.ui.theme.Ink
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Full-width, 56dp-tall, Ink/Cream filled button — the app's primary CTA.
 *       Replaces the inline Button blocks on Login, Register, and Scrapbook
 *       Save. When `loading` is true, renders a Cream spinner in place of the
 *       label and disables the click target so users can't double-submit.
 * Who: Called by every screen whose primary action is a single committing tap.
 * When: Rendered as the final affordance of a form / sheet.
 */
@Composable
fun PrimaryButton(
    label:    String,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
    enabled:  Boolean = true,
    loading:  Boolean = false
) {
    Button(
        onClick  = onClick,
        enabled  = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape  = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor         = Ink,
            contentColor           = Cream,
            disabledContainerColor = BrownDisabled
        )
    ) {
        if (loading) {
            CircularProgressIndicator(color = Cream, modifier = Modifier.size(24.dp))
        } else {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun PrimaryButtonPreview() {
    MemoryCircleTheme {
        PrimaryButton(label = "Sign In", onClick = {})
    }
}
