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
    // Re-bind on every screen entry — heals a couple of failure modes that
    // otherwise leave the calendar empty:
    //   1) The VM was constructed before Firebase Auth had restored the
    //      session, so init's loadMonths() bailed and never came back.
    //   2) A previous group was deleted while Memories was offscreen and the
    //      cascading delete tripped a permission_denied on the now-orphan
    //      scrapbooks listener, which then silently died.
    LaunchedEffect(Unit) { viewModel.bind() }

    val months by viewModel.months.collectAsStateWithLifecycle()

    MemoriesContent(
        months          = months,
        currentRoute    = currentRoute,
        onNavigate      = onNavigate,
        onOpenScrapbook = onOpenScrapbook
    )
}

/**
 * Stateless body — takes the month list + callbacks so it renders in @Preview
 * without touching Firebase. MemoriesScreen above is the thin wrapper that
 * wires the ViewModel.
 */
@Composable
private fun MemoriesContent(
    months:          List<MemoriesViewModel.MonthSection>,
    currentRoute:    String,
    onNavigate:      (Any) -> Unit,
    onOpenScrapbook: (groupId: String, month: String, year: String) -> Unit
) {
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

// ---------------------------------------------------------------------------
// Previews — each one targets the whole screen at a different UI state.
// ---------------------------------------------------------------------------

private val previewMonths = listOf(
    MemoriesViewModel.MonthSection(
        month = "August",
        year  = "2025",
        scrapbooks = listOf(
            MemoriesViewModel.Scrapbook(id = "s1", groupId = "g1", groupName = "Summer Trip",  memoryCount = 12, colorType = "brown", thumbnailUrl = ""),
            MemoriesViewModel.Scrapbook(id = "s2", groupId = "g2", groupName = "Family",       memoryCount = 4,  colorType = "sage",  thumbnailUrl = "")
        )
    ),
    MemoriesViewModel.MonthSection(
        month = "July",
        year  = "2025",
        scrapbooks = listOf(
            MemoriesViewModel.Scrapbook(id = "s3", groupId = "g1", groupName = "Summer Trip",  memoryCount = 6, colorType = "brown", thumbnailUrl = "")
        )
    ),
    MemoriesViewModel.MonthSection(
        month = "June",
        year  = "2025",
        scrapbooks = listOf(
            MemoriesViewModel.Scrapbook(id = "s4", groupId = "g3", groupName = "Weekend Hike", memoryCount = 3, colorType = "brown", thumbnailUrl = ""),
            MemoriesViewModel.Scrapbook(id = "s5", groupId = "g2", groupName = "Family",       memoryCount = 1, colorType = "sage",  thumbnailUrl = "")
        )
    )
)

/** Default — three months of scrapbooks across multiple groups. */
@Preview(showBackground = true, name = "Memories · default")
@Composable
fun MemoriesScreenPreview() {
    MemoryCircleTheme {
        MemoriesContent(
            months          = previewMonths,
            currentRoute    = "memories",
            onNavigate      = {},
            onOpenScrapbook = { _, _, _ -> }
        )
    }
}

/** Empty calendar — no scrapbooks exist yet. */
@Preview(showBackground = true, name = "Memories · empty")
@Composable
fun MemoriesScreenEmptyPreview() {
    MemoryCircleTheme {
        MemoriesContent(
            months          = emptyList(),
            currentRoute    = "memories",
            onNavigate      = {},
            onOpenScrapbook = { _, _, _ -> }
        )
    }
}
