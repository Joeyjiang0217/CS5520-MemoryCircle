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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Displays one month's scrapbook as a vertical timeline. Each entry sits
 *       on a continuous left-hand line marked by a Brown dot and its date, with
 *       a card on the right showing the member photos, an editable title and
 *       description, the mood, and group comments.
 * Who: Called by MemoryCircleNavigation when viewing a scrapbook (ScrapbookViewer route).
 * When: Navigated to from the Memories tab or after generating a scrapbook.
 */
@Composable
fun ScrapbookViewerScreen(
    groupId:     String,
    memberCount: Int,
    onBack:      () -> Unit,
    viewModel:   ScrapbookViewerViewModel = viewModel()
) {
    // Load once (mock data for now); in-memory edits/comments survive recompositions.
    LaunchedEffect(groupId, memberCount) {
        viewModel.loadIfNeeded(groupId, memberCount)
    }
    val entries by viewModel.entries.collectAsStateWithLifecycle()

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
                TimelineEntry(
                    entry        = entry,
                    onSaveText   = { title, desc -> viewModel.updateEntryText(entry.id, title, desc) },
                    onPostComment = { text -> viewModel.addComment(entry.id, author = "You", text = text) }
                )
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
private fun TimelineEntry(
    entry:         ScrapbookEntry,
    onSaveText:    (String, String) -> Unit,
    onPostComment: (String) -> Unit
) {
    // The continuous line is drawn in a background layer that matches the row's
    // height via matchParentSize(). This avoids IntrinsicSize, which OutlinedTextField
    // (used in edit mode) does not support.
    Box(modifier = Modifier.fillMaxWidth()) {

        // --- Background: continuous vertical line in the 72dp left gutter ---
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

        // --- Foreground: left gutter (dot + date) + memory card ---
        Row(modifier = Modifier.fillMaxWidth()) {
            // Dot + date, vertically centered with the card
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

            // Memory card. Bottom padding = spacing to next entry (line spans it too).
            MemoryCard(
                entry         = entry,
                onSaveText    = onSaveText,
                onPostComment = onPostComment,
                modifier      = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, bottom = 20.dp)
            )
        }
    }
}

/**
 * What: The card for a single memory — member photos, an editable title and
 *       description, the mood chip, and the group comment thread.
 * Who: Called by TimelineEntry.
 * When: Rendered for every entry.
 */
@Composable
private fun MemoryCard(
    entry:         ScrapbookEntry,
    onSaveText:    (String, String) -> Unit,
    onPostComment: (String) -> Unit,
    modifier:      Modifier = Modifier
) {
    // Per-entry local UI state (keyed by id so it resets if the list changes)
    var isEditing    by remember(entry.id) { mutableStateOf(false) }
    var titleInput   by remember(entry.id) { mutableStateOf(entry.title) }
    var descInput    by remember(entry.id) { mutableStateOf(entry.description) }
    var commentInput by remember(entry.id) { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = Sage,
        unfocusedBorderColor = Beige
    )

    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = Cream)
    ) {
        // One photo per member — laid out as a grid so every member's photo
        // stays clearly visible regardless of group size.
        MemberPhotoGrid(
            photos   = entry.memberPhotos,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {

            // Header: member count + Edit/Done toggle (pen icon as ✏️)
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = "👥 ${entry.memberPhotos.size} members",
                    style = MaterialTheme.typography.labelSmall,
                    color = Brown
                )
                TextButton(
                    onClick = {
                        if (isEditing) {
                            onSaveText(titleInput, descInput)
                            isEditing = false
                        } else {
                            titleInput = entry.title
                            descInput  = entry.description
                            isEditing  = true
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text  = if (isEditing) "✓ Done" else "✏️ Edit",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title + description: read-only text, or editable fields in edit mode
            if (isEditing) {
                OutlinedTextField(
                    value         = titleInput,
                    onValueChange = { titleInput = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Title") },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = fieldColors
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value         = descInput,
                    onValueChange = { descInput = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Content") },
                    shape         = RoundedCornerShape(12.dp),
                    colors        = fieldColors
                )
            } else {
                Text(
                    text  = entry.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink
                )
                if (entry.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = entry.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mood chip
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

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Beige.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))

            // --- Comments (any group member can share their mood) ---
            Text(
                text  = "COMMENTS",
                style = MaterialTheme.typography.labelSmall,
                color = InkSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (entry.comments.isEmpty()) {
                Text(
                    text  = "No comments yet — be the first to share how you felt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkTertiary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.comments.forEach { comment ->
                        CommentRow(comment)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Comment input
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = commentInput,
                    onValueChange = { commentInput = it },
                    modifier      = Modifier.weight(1f),
                    placeholder   = { Text("Share your mood…", style = MaterialTheme.typography.bodyMedium) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(20.dp),
                    colors        = fieldColors
                )
                TextButton(onClick = {
                    onPostComment(commentInput)
                    commentInput = ""
                }) {
                    Text("Post", color = AccentGreen)
                }
            }
        }
    }
}

/**
 * What: A single comment line — author in bold followed by their text.
 * Who: Called by MemoryCard for each comment.
 * When: Rendered for every comment on an entry.
 */
@Composable
private fun CommentRow(comment: Comment) {
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

/**
 * What: Lays out one photo per group member in a grid sized to the count, so
 *       every member's photo stays large enough to see. Cells have a fixed
 *       height and crop to fill.
 * Who: Called by MemoryCard for each entry's member photos.
 * When: Rendered inside every memory card.
 */
@Composable
private fun MemberPhotoGrid(
    photos:   List<String>,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) return

    // Fewer columns for small groups keeps photos big; cap at 3 for larger ones.
    val columns = when (photos.size) {
        1    -> 1
        2, 4 -> 2
        else -> 3   // 3, 5, 6
    }

    Column(
        modifier            = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        photos.chunked(columns).forEach { rowPhotos ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowPhotos.forEach { url ->
                    AsyncImage(
                        model              = url,
                        contentDescription = "Member photo",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                // Keep cell widths aligned when the last row isn't full
                repeat(columns - rowPhotos.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScrapbookViewerScreenPreview() {
    MemoryCircleTheme {
        ScrapbookViewerScreen(groupId = "test", memberCount = 4, onBack = {})
    }
}
