/**
 * What: The MemoryCircle brand color palette (named Color constants and their
 *       transparency/destructive variants).
 * Who:  Used by Theme.kt to build the Material color scheme and referenced
 *       directly by composables across the ui package for custom coloring.
 * When: Resolved at composition time wherever a color constant is read.
 */

package com.cs5520group15.memorycircle.ui.theme

import androidx.compose.ui.graphics.Color

// MemoryCircle Brand Colors
val Cream       = Color(0xFFF8F4EE)
val Sage        = Color(0xFFAFC8AD)
val Beige       = Color(0xFFDCCFC0)
val Brown       = Color(0xFFB79F8A)
val GraySoft    = Color(0xFFF1F1F1)
val Ink         = Color(0xFF3A332B)
val AccentGreen = Color(0xFF7C9C7A)

// Transparency variants
val BrownDisabled = Color(0x66B79F8A)  // Brown at 40% opacity
val InkSecondary  = Color(0xB33A332B)  // Ink at 70% opacity
val InkTertiary   = Color(0x993A332B)  // Ink at 60% opacity
val WhiteCard     = Color(0xCCFFFFFF)  // White at 80% opacity

// Destructive / warning
val DeleteRed   = Color(0xFFC25B5B)