package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme
import com.cs5520group15.memorycircle.ui.theme.Sage
import com.cs5520group15.memorycircle.ui.theme.Cream

/**
 * What: Displays a circular avatar. When `photoUrl` is non-blank we render
 *       the URL via Coil's AsyncImage; otherwise we fall back to the original
 *       letter avatar (first letter of `name` on a Sage circle). Letter size
 *       scales linearly with the circle diameter (40 % of size) so a 240dp
 *       avatar carries a properly weighted glyph instead of a 16sp pinhead
 *       in the middle.
 * Who: Called by HomeScreen (top right), ProfileScreen, AvatarViewerScreen,
 *      Friends / Group rows, etc.
 * When: Rendered whenever a user's avatar is needed. Callers that have a
 *       photoUrl in hand should pass it; callers that don't (group rows,
 *       friend rows that still use mock avatars) keep the letter fallback
 *       by omitting the argument.
 */
@Composable
fun AvatarCircle(
    name:     String,
    size:     Dp     = 40.dp,
    photoUrl: String? = null
) {
    if (!photoUrl.isNullOrBlank()) {
        AsyncImage(
            model              = photoUrl,
            contentDescription = "$name's avatar",
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Sage)   // visible while the image loads
        )
        return
    }

    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val glyphSize = (size.value * 0.4f).sp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Sage)
    ) {
        Text(
            text  = initial,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = glyphSize),
            color = Cream
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AvatarCirclePreview() {
    MemoryCircleTheme {
        AvatarCircle(name = "Sarah")
    }
}
