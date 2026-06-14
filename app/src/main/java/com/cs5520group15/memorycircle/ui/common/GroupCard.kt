package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.ui.theme.Brown
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme
import com.cs5520group15.memorycircle.ui.theme.Sage

/**
 * What: Hero gradient card for a memory group on HomeScreen — a left-anchored
 *       date + name column with a memory-count badge floated to the top right.
 *       Gradient color follows `colorType` ("sage" or "brown") so different
 *       groups visually distinguish at a glance.
 * Who: Called by HomeScreen inside the RECENT GROUPS LazyColumn.
 * When: Rendered for each group on the Home tab.
 */
@Composable
fun GroupCard(
    name:        String,
    date:        String,
    memoryCount: Int,
    colorType:   String,
    onClick:     () -> Unit,
    modifier:    Modifier = Modifier
) {
    val gradientColors = if (colorType == "sage") {
        listOf(Sage, Sage.copy(alpha = 0.7f))
    } else {
        listOf(Brown, Brown.copy(alpha = 0.7f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                text  = date,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text  = "📷 $memoryCount",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GroupCardPreview() {
    MemoryCircleTheme {
        GroupCard(
            name        = "Summer Picnic",
            date        = "June 1, 2025",
            memoryCount = 12,
            colorType   = "brown",
            onClick     = {}
        )
    }
}
