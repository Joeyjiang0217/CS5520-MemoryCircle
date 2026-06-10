package com.cs5520group15.memorycircle.ui.scrapbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cs5520group15.memorycircle.BuildConfig
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Displays one month's scrapbook as a vertical timeline. Each entry sits
 *       on a continuous left-hand line marked by a Brown dot and its date, with
 *       a card on the right showing the photo, title, description, and mood.
 * Who: Called by MemoryCircleNavigation when viewing a scrapbook (ScrapbookViewer route).
 * When: Navigated to from the Memories tab or after generating a scrapbook.
 */
@Composable
fun ScrapbookViewerScreen(
    groupId: String,
    onBack:  () -> Unit
) {
    // In debug builds, load mock data instead of Firestore.
    val entries = remember(groupId) {
        if (BuildConfig.DEBUG) {
            ScrapbookMockData.getMockEntries(groupId)
        } else {
            // TODO: load this month's entries from Firestore
            ScrapbookMockData.getMockEntries(groupId)
        }
    }

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "June 2025",
                showBack = true,
                onBack   = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // No verticalArrangement spacing here — each row carries its own
            // bottom padding so the timeline line stays continuous between entries.
            items(entries, key = { it.id }) { entry ->
                TimelineEntry(entry = entry)
            }
        }
    }
}

/**
 * What: One timeline row — a continuous vertical line + dot + date on the left,
 *       and the memory card on the right.
 * Who: Called by ScrapbookViewerScreen for each entry.
 * When: Rendered for every entry in the month.
 */
@Composable
private fun TimelineEntry(entry: ScrapbookEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // IntrinsicSize.Min lets the left line fillMaxHeight to match the card,
            // so the line spans the full row (card + bottom spacing) and connects
            // continuously to the next entry.
            .height(IntrinsicSize.Min)
    ) {
        // --- LEFT: continuous line, dot, date (fixed 72dp) ---
        Box(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
        ) {
            // Continuous vertical line, centered, spanning the full row height
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .align(Alignment.TopCenter)
                    .background(Brown)
            )
            // Dot (centered with the card) + date directly below it
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Brown)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text      = entry.date,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = Brown,
                    textAlign = TextAlign.Center
                )
            }
        }

        // --- RIGHT: memory card (weight 1f). Bottom padding = spacing to next entry ---
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, bottom = 20.dp)
        ) {
            Card(
                shape     = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors    = CardDefaults.cardColors(containerColor = Cream)
            ) {
                AsyncImage(
                    model              = entry.imageUrl,
                    contentDescription = entry.title,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text  = entry.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink
                    )
                    Text(
                        text     = entry.description,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Mood chip — SageGreen isn't a defined theme color, so this
                    // uses AccentGreen (deeper green) for readable white text.
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentGreen)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text  = entry.mood,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScrapbookViewerScreenPreview() {
    MemoryCircleTheme {
        ScrapbookViewerScreen(groupId = "test", onBack = {})
    }
}
