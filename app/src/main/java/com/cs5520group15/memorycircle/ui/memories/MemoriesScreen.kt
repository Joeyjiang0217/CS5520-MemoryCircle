/**
 * What: Jetpack Compose UI for the Memories screen — browses past scrapbooks and
 *       opens them by month/year.
 * Who:  Wired into the nav graph by MemoryCircleNavigation; reached from the bottom
 *       navigation bar's "memories" tab.
 * When: Composed when the user navigates to the Memories route via the bottom nav tab.
 */

package com.cs5520group15.memorycircle.ui.memories

import androidx.compose.foundation.background
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
import com.cs5520group15.memorycircle.ui.common.MonthScrapbookRow
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
            MonthScrapbookRow(
                title        = scrapbook.groupName,
                memoryCount  = scrapbook.memoryCount,
                colorType    = scrapbook.colorType,
                thumbnailUrl = scrapbook.thumbnailUrl,
                onClick      = { onOpenScrapbook(scrapbook) }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun MonthSectionPreview() {
    MemoryCircleTheme {
        MonthSection(
            section = MemoriesViewModel.MonthSection(
                month = "June",
                year  = "2025",
                scrapbooks = listOf(
                    MemoriesViewModel.Scrapbook(
                        id           = "s1",
                        groupId      = "g1",
                        groupName    = "Summer Trip",
                        memoryCount  = 8,
                        colorType    = "brown",
                        thumbnailUrl = ""
                    )
                )
            ),
            onOpenScrapbook = {}
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
