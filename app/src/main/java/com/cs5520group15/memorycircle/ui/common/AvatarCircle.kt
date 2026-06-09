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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme
import com.cs5520group15.memorycircle.ui.theme.Sage
import com.cs5520group15.memorycircle.ui.theme.Cream

/**
 * What: Displays a circular avatar showing the first letter of the user's name.
 * Who: Called by HomeScreen (top right) and ProfileScreen.
 * When: Rendered whenever a user's avatar is needed and no profile photo is available.
 */
@Composable
fun AvatarCircle(
    name: String,
    size: Dp = 40.dp
) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Sage)
    ) {
        Text(
            text  = initial,
            style = MaterialTheme.typography.labelLarge,
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