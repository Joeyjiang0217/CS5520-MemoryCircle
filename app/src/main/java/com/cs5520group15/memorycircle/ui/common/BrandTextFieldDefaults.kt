/**
 * What: Shared brand TextField color palettes — a transparent default and a
 *       translucent-white-on-gradient variant.
 * Who:  Used by CreateGroupScreen, EditProfileScreen, ScrapbookScreen/ScrapbookViewerScreen
 *       (default) and LoginScreen, RegisterScreen (on-gradient).
 * When: Passed to an OutlinedTextField's `colors` parameter when composing input fields.
 */

package com.cs5520group15.memorycircle.ui.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cs5520group15.memorycircle.ui.theme.Beige
import com.cs5520group15.memorycircle.ui.theme.Sage

/**
 * What: Brand-coloured OutlinedTextField palette (focused = Sage, unfocused = Beige).
 *       Centralised so every TextField across the app stays visually consistent.
 *       Two flavours exist because Login / Register / Scrapbook also want a
 *       translucent white container; everywhere else accepts the default
 *       transparent fill.
 * Who: Called by any screen that hosts an OutlinedTextField.
 * When: Passed to OutlinedTextField's `colors` parameter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun brandFieldColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = Sage,
        unfocusedBorderColor = Beige
    )

/**
 * What: Brand text field colors with a translucent white container — used on
 *       login/register/scrapbook forms where the field sits on a Beige→Cream
 *       gradient and needs an opaque-ish surface to stay readable.
 * Who: Called by LoginScreen, RegisterScreen, and the ScrapbookScreen description box.
 * When: Passed to OutlinedTextField's `colors` parameter on those screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun brandFieldColorsOnGradient(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = Sage,
        unfocusedBorderColor    = Beige,
        focusedContainerColor   = Color.White.copy(alpha = 0.8f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.8f)
    )
