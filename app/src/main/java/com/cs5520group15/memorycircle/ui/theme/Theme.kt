package com.cs5520group15.memorycircle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MemoryCircleColorScheme = lightColorScheme(
    background     = Cream,
    surface        = Cream,
    surfaceVariant = GraySoft,
    primary        = Brown,
    secondary      = Sage,
    onBackground   = Ink,
    onSurface      = Ink,
    onPrimary      = Cream,
    onSecondary    = Ink,
    outline        = Beige,
    error          = Color(0xFFB00020)
)

/**
 * What: The root Material3 theme for MemoryCircle.
 * Who: Called by MainActivity to wrap the entire app.
 * When: Applied once at app startup; all child composables inherit colors and typography.
 */
@Composable
fun MemoryCircleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MemoryCircleColorScheme,
        typography  = MemoryCircleTypography,
        content     = content
    )
}