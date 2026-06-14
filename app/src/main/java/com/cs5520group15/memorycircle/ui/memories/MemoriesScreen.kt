package com.cs5520group15.memorycircle.ui.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.ui.common.MemoryCircleBottomNav
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Memories tab screen shown as a calendar.
 *       Scrapbooks are generated per month, so they are grouped under each month
 *       (e.g. January → Group 1, Group 2 scrapbooks). This tab is read-only —
 *       tapping a scrapbook opens its timeline; new memories are created from a
 *       group's timeline, not here.
 * Who: Called by MemoryCircleNavigation when the user taps the Memories tab.
 * When: Displayed when the Memories tab is active in the bottom nav.
 */
@Composable
fun MemoriesScreen(
    currentRoute:    String,
    onNavigate:      (Any) -> Unit,
    onOpenScrapbook: (groupId: String, month: String, year: String) -> Unit,
    viewModel:       MemoriesViewModel = viewModel()
) {
    val months by viewModel.months.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(title = "Memories")
        },
        bottomBar = {
            MemoryCircleBottomNav(
                currentRoute = currentRoute,
                onNavigate   = onNavigate
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding      = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            item {
                Text(
                    text  = "YOUR SCRAPBOOK CALENDAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
            }

            // One section per month, each holding that month's scrapbooks
            items(months, key = { it.month + it.year }) { section ->
                MonthSection(
                    section         = section,
                    onOpenScrapbook = { scrapbook ->
                        onOpenScrapbook(scrapbook.groupId, section.month, section.year)
                    }
                )
            }
        }
    }
}

/**
 * What: Renders one month bucket — a month header followed by its scrapbook cards.
 * Who: Called by MemoriesScreen for each month in the calendar.
 * When: Rendered for every month that has scrapbooks.
 */
@Composable
private fun MonthSection(
    section:         MemoriesViewModel.MonthSection,
    onOpenScrapbook: (MemoriesViewModel.Scrapbook) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Month header — calendar-style badge + month/year
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Beige.copy(alpha = 0.4f))
            ) {
                Text(
                    text  = section.month.take(3).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Brown
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text  = section.month,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink
                )
                Text(
                    text  = section.year,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTertiary
                )
            }
        }

        // Scrapbooks generated for this month (one per group)
        section.scrapbooks.forEach { scrapbook ->
            ScrapbookCard(
                scrapbook = scrapbook,
                onClick   = { onOpenScrapbook(scrapbook) }
            )
        }
    }
}

/**
 * What: A single generated scrapbook card inside a month section.
 *       Shows which group it belongs to and how many memories it holds.
 * Who: Called by MonthSection for each scrapbook.
 * When: Rendered for every scrapbook in a month.
 */
@Composable
private fun ScrapbookCard(
    scrapbook: MemoriesViewModel.Scrapbook,
    onClick:   () -> Unit
) {
    val accent = if (scrapbook.colorType == "sage") Sage else Brown

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WhiteCard)
            .border(1.dp, Beige.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Color accent chip
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.85f))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = scrapbook.groupName,
                style = MaterialTheme.typography.titleLarge,
                color = Ink
            )
            Text(
                text  = "📷 ${scrapbook.memoryCount} memories",
                style = MaterialTheme.typography.bodyMedium,
                color = InkTertiary
            )
        }
        Text(
            text  = "›",
            style = MaterialTheme.typography.headlineMedium,
            color = Brown
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MemoriesScreenPreview() {
    MemoryCircleTheme {
        MemoriesScreen(
            currentRoute    = "memories",
            onNavigate      = {},
            onOpenScrapbook = { _, _, _ -> }
        )
    }
}
