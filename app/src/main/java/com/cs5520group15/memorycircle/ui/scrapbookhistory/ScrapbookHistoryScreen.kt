package com.cs5520group15.memorycircle.ui.scrapbookhistory

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cs5520group15.memorycircle.data.ScrapbookRepository
import com.cs5520group15.memorycircle.model.Comment
import com.cs5520group15.memorycircle.model.MemberContribution
import com.cs5520group15.memorycircle.model.ScrapbookEntry
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Read-only historical view of a single month's scrapbook for one group.
 *       Shares the same timeline visual as ScrapbookViewerScreen (continuous
 *       Brown gutter line, day dots, member-contribution cards) but strips
 *       every interactive affordance — no Edit, no "Add my photo", no comment
 *       input, no FAB, no menu icon. The top bar carries only a back button.
 * Who: Called by MemoryCircleNavigation for the ScrapbookHistory route.
 * When: Opened from the Memories tab and from the per-month list on GroupDetail.
 *
 * @param groupId the group whose past timeline is being shown
 * @param month   e.g. "March" — drives the top-bar title
 * @param year    e.g. "2025"  — drives the top-bar title
 */
@Composable
fun ScrapbookHistoryScreen(
    groupId: String,
    month:   String,
    year:    String,
    onBack:  () -> Unit
) {
    val entriesFlow = remember(groupId) { ScrapbookRepository.entriesFor(groupId) }
    val entries by entriesFlow.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "$month $year",
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
            items(entries, key = { it.id }) { entry ->
                HistoryTimelineEntry(entry = entry)
            }
        }
    }
}

@Composable
private fun HistoryTimelineEntry(entry: ScrapbookEntry) {
    Box(modifier = Modifier.fillMaxWidth()) {

        Box(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Brown)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier
                    .width(72.dp)
                    .align(Alignment.CenterVertically)
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

            HistoryMemoryCard(
                entry    = entry,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, bottom = 20.dp)
            )
        }
    }
}

@Composable
private fun HistoryMemoryCard(
    entry:    ScrapbookEntry,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = Cream)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Text(
                text  = "👥 ${entry.contributions.size} members",
                style = MaterialTheme.typography.labelSmall,
                color = Brown
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text  = entry.title,
                style = MaterialTheme.typography.titleLarge,
                color = Ink
            )

            if (entry.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = entry.tags.joinToString("  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                entry.contributions.forEach { contribution ->
                    HistoryContributionBlock(contribution)
                }
            }

            if (entry.comments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Beige.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text  = "COMMENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.comments.forEach { comment -> HistoryCommentRow(comment) }
                }
            }
        }
    }
}

@Composable
private fun HistoryContributionBlock(contribution: MemberContribution) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsyncImage(
            model              = contribution.photoUri,
            contentDescription = "${contribution.memberName}'s photo",
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Row(verticalAlignment = Alignment.Top) {
            AvatarCircle(name = contribution.memberName, size = 28.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text  = contribution.memberName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Brown
                )
                if (contribution.description.isNotBlank()) {
                    Text(
                        text  = contribution.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCommentRow(comment: Comment) {
    Row {
        Text(
            text  = comment.author,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = Brown
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text  = comment.text,
            style = MaterialTheme.typography.bodySmall,
            color = Ink
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun ScrapbookHistoryScreenPreview() {
    MemoryCircleTheme {
        ScrapbookHistoryScreen(
            groupId = "1",
            month   = "March",
            year    = "2025",
            onBack  = {}
        )
    }
}
