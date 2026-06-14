package com.cs5520group15.memorycircle.ui.scrapbookviewer

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.data.CurrentUser
import com.cs5520group15.memorycircle.data.ScrapbookRepository
import com.cs5520group15.memorycircle.model.Comment
import com.cs5520group15.memorycircle.model.MemberContribution
import com.cs5520group15.memorycircle.model.ScrapbookEntry
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.EmptyHint
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.common.SecondaryOutlinedButton
import com.cs5520group15.memorycircle.ui.common.brandFieldColors
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Displays one group's collaborative timeline. Each time point sits on a
 *       continuous left-hand line marked by a Brown dot and its date, with a card
 *       on the right showing every member's photo + avatar + name + their own
 *       description, an editable title, group comments, and an "Add my photo" CTA.
 *       A FAB adds a brand-new time point.
 * Who: Called by MemoryCircleNavigation for the ScrapbookViewer route.
 * When: Opened from a Home group card or the Memories tab.
 */
@Composable
fun ScrapbookViewerScreen(
    groupId:           String,
    onBack:            () -> Unit,
    onOpenGroupDetail: () -> Unit,
    onAddTimePoint:    () -> Unit,
    onJoinEntry:       (String) -> Unit,
    viewModel:         ScrapbookViewerViewModel = viewModel()
) {
    LaunchedEffect(groupId) { viewModel.bind(groupId) }

    val entriesFlow = remember(groupId) { ScrapbookRepository.entriesFor(groupId) }
    val entries by entriesFlow.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "June 2025",
                showBack = true,
                onBack   = onBack,
                actions  = {
                    IconButton(onClick = onOpenGroupDetail) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_menu),
                            contentDescription = "Open group details"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onAddTimePoint,
                containerColor = Ink,
                contentColor   = Cream
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                EmptyHint(
                    text = "No memories yet — press + to add your first one."
                )
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
                TimelineEntry(
                    entry         = entry,
                    onSaveTitle   = { title -> viewModel.updateEntryTitle(entry.id, title) },
                    onPostComment = { text -> viewModel.addComment(entry.id, author = CurrentUser.name, text = text) },
                    onJoin        = { onJoinEntry(entry.id) }
                )
            }
        }
    }
}

@Composable
private fun TimelineEntry(
    entry:         ScrapbookEntry,
    onSaveTitle:   (String) -> Unit,
    onPostComment: (String) -> Unit,
    onJoin:        () -> Unit
) {
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

            MemoryCard(
                entry         = entry,
                onSaveTitle   = onSaveTitle,
                onPostComment = onPostComment,
                onJoin        = onJoin,
                modifier      = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, bottom = 20.dp)
            )
        }
    }
}

@Composable
private fun MemoryCard(
    entry:         ScrapbookEntry,
    onSaveTitle:   (String) -> Unit,
    onPostComment: (String) -> Unit,
    onJoin:        () -> Unit,
    modifier:      Modifier = Modifier
) {
    var isEditing    by remember(entry.id) { mutableStateOf(false) }
    var titleInput   by remember(entry.id) { mutableStateOf(entry.title) }
    var commentInput by remember(entry.id) { mutableStateOf("") }

    val fieldColors = brandFieldColors()

    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = Cream)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = "👥 ${entry.contributions.size} members",
                    style = MaterialTheme.typography.labelSmall,
                    color = Brown
                )
                TextButton(
                    onClick = {
                        if (isEditing) {
                            onSaveTitle(titleInput)
                            isEditing = false
                        } else {
                            titleInput = entry.title
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
            } else {
                Text(
                    text  = entry.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink
                )
            }

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
                    ContributionBlock(contribution)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SecondaryOutlinedButton(
                label        = "➕  Add my photo",
                onClick      = onJoin,
                borderColor  = AccentGreen.copy(alpha = 0.5f),
                contentColor = AccentGreen
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Beige.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))

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
                    entry.comments.forEach { comment -> CommentRow(comment) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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

@Composable
private fun ContributionBlock(contribution: MemberContribution) {
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

@Preview(showBackground = true)
@Composable
fun ScrapbookViewerScreenPreview() {
    MemoryCircleTheme {
        ScrapbookViewerScreen(
            groupId           = "test",
            onBack            = {},
            onOpenGroupDetail = {},
            onAddTimePoint    = {},
            onJoinEntry       = {}
        )
    }
}
