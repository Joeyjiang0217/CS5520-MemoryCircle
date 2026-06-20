/**
 * What: The app's font families (Cormorant Garamond, DM Sans) and the
 *       MemoryCircleTypography text-style scale built from them.
 * Who:  Used by Theme.kt to set MaterialTheme.typography; styles are then read
 *       by every composable via MaterialTheme.typography.
 * When: Resolved at composition time wherever a typography style is read.
 */

package com.cs5520group15.memorycircle.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cs5520group15.memorycircle.R

// Cormorant Garamond — serif font for headings and display text
val CormorantGaramond = FontFamily(
    Font(R.font.cormorant_garamond_medium,   FontWeight.Medium),
    Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_bold,     FontWeight.Bold)
)

// DM Sans — sans-serif font for body text and UI elements
val DMSans = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium,  FontWeight.Medium),
    Font(R.font.dmsans_bold,    FontWeight.Bold)
)

val MemoryCircleTypography = Typography(
    // Brand display title (e.g. "MemoryCircle" on the login screen)
    displayLarge = TextStyle(
        fontFamily    = CormorantGaramond,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 48.sp,
        letterSpacing = 0.sp
    ),
    // Page-level headings (e.g. "New Scrapbook")
    headlineMedium = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 28.sp
    ),
    // Card titles (e.g. "Summer Picnic")
    titleLarge = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.Medium,
        fontSize   = 22.sp
    ),
    // Standard body text
    bodyLarge = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp
    ),
    // Form field labels (e.g. EMAIL, PASSWORD — uppercase + wide tracking)
    labelSmall = TextStyle(
        fontFamily    = DMSans,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        letterSpacing = 1.5.sp
    ),
    // Button text
    labelLarge = TextStyle(
        fontFamily    = DMSans,
        fontWeight    = FontWeight.Bold,
        fontSize      = 16.sp,
        letterSpacing = 0.5.sp
    )
)