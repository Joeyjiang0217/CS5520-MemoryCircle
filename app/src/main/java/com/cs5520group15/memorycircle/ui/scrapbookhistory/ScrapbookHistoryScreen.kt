/**
 * What: Jetpack Compose UI for the Scrapbook History screen, showing a group's scrapbook
 *       entries for a given month and year.
 * Who:  Wired into the nav graph by MemoryCircleNavigation under the ScrapbookHistory route;
 *       reached from MemoriesScreen and GroupDetailScreen by opening a scrapbook.
 * When: Composed when the user navigates to the ScrapbookHistory destination.
 */

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
import coil.compose.AsyncImage
import com.cs5520group15.memorycircle.data.ScrapbookRepository
import com.cs5520group15.memorycircle.model.Comment
import com.cs5520group15.memorycircle.model.Photo
import com.cs5520group15.memorycircle.model.ScrapbookEntry
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.EmptyHint
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*
import java.time.Month
import java.time.YearMonth
import java.util.Locale

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
    // Map ("March", "2025") → "2025-03" so we can hit the right Firestore doc.
    val scrapbookId = remember(month, year) {
        runCatching {
            val m = Month.valueOf(month.uppercase(Locale.ENGLISH)).value
            YearMonth.of(year.toInt(), m).toString()
        }.getOrDefault(YearMonth.now().toString())
    }

    var entries by remember(groupId, scrapbookId) { mutableStateOf<List<ScrapbookEntry>>(emptyList()) }
    var loading by remember(groupId, scrapbookId) { mutableStateOf(true) }

    LaunchedEffect(groupId, scrapbookId) {
        loading = true
        entries = runCatching { ScrapbookRepository.loadMonthEntries(groupId, scrapbookId) }
            .getOrDefault(emptyList())
        loading = false
    }

    ScrapbookHistoryContent(
        month   = month,
        year    = year,
        entries = entries,
        loading = loading,
        onBack  = onBack
    )
}

/**
 * Stateless body — takes the entry list + loading flag so it renders in
 * @Preview without touching Firebase. ScrapbookHistoryScreen above is the thin
 * wrapper that resolves the month → scrapbookId map and fetches via the
 * repository.
 */
@Composable
private fun ScrapbookHistoryContent(
    month:   String,
    year:    String,
    entries: List<ScrapbookEntry>,
    loading: Boolean,
    onBack:  () -> Unit
) {
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
        if (!loading && entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                EmptyHint(text = "No memories were captured this month.")
            }
            return@Scaffold
        }

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
                text  = "📸 ${entry.photos.size} photos",
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
                entry.photos.forEach { photo ->
                    HistoryPhotoBlock(
                        photo             = photo,
                        fallbackName      = entry.authorName,
                        fallbackAvatarUrl = entry.authorAvatarUrl
                    )
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
private fun HistoryPhotoBlock(photo: Photo, fallbackName: String, fallbackAvatarUrl: String) {
    val displayName   = photo.uploaderName.ifBlank { fallbackName }
    val displayAvatar = photo.uploaderAvatarUrl.ifBlank { fallbackAvatarUrl }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsyncImage(
            model              = photo.url,
            contentDescription = "$displayName's photo",
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Row(verticalAlignment = Alignment.Top) {
            AvatarCircle(name = displayName, size = 28.dp, photoUrl = displayAvatar)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text  = displayName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Brown
                )
                if (photo.description.isNotBlank()) {
                    Text(
                        text  = photo.description,
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
            text  = comment.authorName,
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

// ---------------------------------------------------------------------------
// Previews — each one targets the whole screen at a different UI state.
// ---------------------------------------------------------------------------

private val previewPhoto = Photo(
    photoId           = "p1",
    url               = "",
    storagePath       = "",
    description       = "Sunset at the lake",
    uploaderId        = "u1",
    uploaderName      = "Ada",
    uploaderAvatarUrl = ""
)

private val previewComment = Comment(
    id              = "c1",
    authorId        = "u1",
    authorName      = "Ada Lovelace",
    authorAvatarUrl = "",
    text            = "So much fun!"
)

private val previewEntries = listOf(
    ScrapbookEntry(
        id              = "e1",
        authorId        = "u1",
        authorName      = "Ada Lovelace",
        authorAvatarUrl = "",
        date            = "June 1",
        title           = "Lakeside Picnic",
        tags            = listOf("food", "park"),
        photos          = listOf(previewPhoto),
        comments        = listOf(previewComment),
        commentCount    = 1
    ),
    ScrapbookEntry(
        id              = "e2",
        authorId        = "u2",
        authorName      = "Grace Hopper",
        authorAvatarUrl = "",
        date            = "June 14",
        title           = "Sunrise Hike",
        tags            = listOf("hike"),
        photos          = listOf(previewPhoto.copy(photoId = "p2", description = "Top of the hill")),
        comments        = emptyList(),
        commentCount    = 0
    )
)

/** Default — two timeline entries, the first with a comment. */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Scrapbook history · default")
@Composable
fun ScrapbookHistoryScreenPreview() {
    MemoryCircleTheme {
        ScrapbookHistoryContent(
            month   = "June",
            year    = "2025",
            entries = previewEntries,
            loading = false,
            onBack  = {}
        )
    }
}

/** Empty — month resolved but no entries exist. */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Scrapbook history · empty")
@Composable
fun ScrapbookHistoryScreenEmptyPreview() {
    MemoryCircleTheme {
        ScrapbookHistoryContent(
            month   = "March",
            year    = "2025",
            entries = emptyList(),
            loading = false,
            onBack  = {}
        )
    }
}

/** Loading — entries being fetched. */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Scrapbook history · loading")
@Composable
fun ScrapbookHistoryScreenLoadingPreview() {
    MemoryCircleTheme {
        ScrapbookHistoryContent(
            month   = "June",
            year    = "2025",
            entries = emptyList(),
            loading = true,
            onBack  = {}
        )
    }
}
