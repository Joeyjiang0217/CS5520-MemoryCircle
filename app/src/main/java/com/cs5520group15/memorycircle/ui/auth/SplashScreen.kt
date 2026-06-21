/**
 * What: The custom Compose splash screen — a full-screen cream surface showing
 *       the MemoryCircle logo with the "MEMORYCIRCLE" wordmark sliding in, then
 *       auto-advancing to the auth/login flow.
 * Who:  Registered as the start destination by MemoryCircleNavigation; replaces
 *       the old core-splashscreen (windowSplashScreen) implementation.
 * When: Composed once at cold start; fires onTimeout after the intro animation
 *       (2.5s total) so the nav layer can move on to Login/Home.
 */

package com.cs5520group15.memorycircle.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.theme.CormorantGaramond
import com.cs5520group15.memorycircle.ui.theme.Cream
import kotlinx.coroutines.delay

// Brand brown used for the wordmark on the splash. Darker than the palette's
// `Brown` so the text reads clearly against the cream background.
private val SplashBrown = Color(0xFF5C4A32)

// Total time the splash is shown before handing off to the next screen.
private const val SPLASH_DURATION_MS = 2_500L

/**
 * What: Full-screen branded splash — centered logo plus an animated wordmark.
 * Who: Hosted by MemoryCircleNavigation as the start destination.
 * When: Shown at app launch; invokes [onTimeout] after [SPLASH_DURATION_MS].
 */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Capture the latest callback so the timer below isn't restarted if the
    // lambda identity changes across recompositions.
    val currentOnTimeout by rememberUpdatedState(onTimeout)

    // Drives the AnimatedVisibility; flipped true on first composition so the
    // wordmark animates in rather than appearing already-visible.
    var showWordmark by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showWordmark = true
        delay(SPLASH_DURATION_MS)
        currentOnTimeout()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_memorycircle_logo),
            contentDescription = "MemoryCircle logo",
            modifier = Modifier.size(160.dp)
        )

        AnimatedVisibility(
            visible = showWordmark,
            enter = slideInHorizontally(
                animationSpec = tween(durationMillis = 700),
                // Start fully off-screen to the left, slide to resting position.
                initialOffsetX = { fullWidth -> -fullWidth }
            ) + fadeIn(animationSpec = tween(durationMillis = 700))
        ) {
            Text(
                text = "MEMORYCIRCLE",
                color = SplashBrown,
                fontFamily = CormorantGaramond,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                letterSpacing = 4.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    SplashScreen(onTimeout = {})
}
