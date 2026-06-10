package com.cs5520group15.memorycircle.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Displays a single memory group as a gradient card with name, date, and memory count.
 * Who: Called by HomeScreen inside a LazyColumn.
 * When: Rendered for each group in the groups list.
 */
@Composable
fun GroupCard(
    group:   HomeViewModel.Group,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Choose gradient colors based on colorType
    val gradientColors = if (group.colorType == "sage") {
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
        // Date and group name (left side)
        Column(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(
                text  = group.date,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = group.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        // Memory count badge (top right)
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
                text  = "📷 ${group.memoryCount}",
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
            group = HomeViewModel.Group(
                id          = "1",
                name        = "Summer Picnic",
                date        = "June 1, 2025",
                memoryCount = 12,
                colorType   = "brown"
            ),
            onClick = {}
        )
    }
}