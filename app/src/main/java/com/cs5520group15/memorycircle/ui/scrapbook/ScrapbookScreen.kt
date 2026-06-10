package com.cs5520group15.memorycircle.ui.scrapbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Scrapbook creation screen where users configure and generate a memory scrapbook.
 *       Includes collaborator selection, tags, template preview, journal entry, and generate button.
 * Who: Called by MemoryCircleNavigation when user taps a group card or the FAB on HomeScreen.
 * When: Displayed when navigating to ScrapbookDetail route.
 */
@Composable
fun ScrapbookScreen(
    groupId:  String,
    onBack:   () -> Unit,
    onGenerate: (Int) -> Unit = {},
    viewModel: ScrapbookViewModel = viewModel()
) {
    val selectedGroupId     by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    val selectedMemberCount by viewModel.selectedMemberCount.collectAsStateWithLifecycle()
    val journalEntry        by viewModel.journalEntry.collectAsStateWithLifecycle()
    val tags                by viewModel.tags.collectAsStateWithLifecycle()

    var newTagInput by remember { mutableStateOf("") }
    var showAddTag  by remember { mutableStateOf(false) }

    // If we arrived from a specific group card, pre-select that group.
    // The Memories "+" flow passes "new", which matches nothing, so nothing is preselected.
    LaunchedEffect(groupId) {
        if (viewModel.availableGroups.any { it.id == groupId }) {
            viewModel.onSelectGroup(groupId)
        }
    }

    // Dummy collaborator colors for UI skeleton
    val collaboratorColors = listOf(Sage, Beige, Brown)

    Scaffold(
        containerColor = Cream,
        topBar = {
            com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar(
                title    = "New Scrapbook",
                showBack = true,
                onBack   = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- Select Group ---
            // Single-select: only one existing group can be the scrapbook source.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text  = "SELECT GROUP",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.availableGroups.forEach { group ->
                        GroupSelectChip(
                            label      = group.name,
                            isSelected = selectedGroupId == group.id,
                            onClick    = { viewModel.onSelectGroup(group.id) }
                        )
                    }
                }
            }

            // --- Collaborate With ---
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text  = "COLLABORATE WITH",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Dummy collaborator avatars
                    collaboratorColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                    // Add collaborator button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Beige, CircleShape)
                            .clickable { }
                    ) {
                        Text("+", color = Brown, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            // --- Tags ---
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text  = "TAGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
                // Existing tags wrap onto multiple lines as needed
                FlowRow(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        TagChip(
                            label     = tag,
                            onRemove  = { viewModel.onRemoveTag(tag) }
                        )
                    }
                    if (!showAddTag) {
                        AddTagChip(onClick = { showAddTag = true })
                    }
                }

                // Tag input gets its own full-width row so the typed text is
                // never clipped (no forced height/width on the text field).
                if (showAddTag) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value         = newTagInput,
                            onValueChange = { newTagInput = it },
                            modifier      = Modifier.weight(1f),
                            shape         = RoundedCornerShape(20.dp),
                            singleLine    = true,
                            placeholder   = { Text("Enter a tag", style = MaterialTheme.typography.bodyMedium) },
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Sage,
                                unfocusedBorderColor = Beige
                            )
                        )
                        TextButton(onClick = {
                            viewModel.onAddTag(newTagInput)
                            newTagInput = ""
                            showAddTag  = false
                        }) {
                            Text("Add", color = AccentGreen)
                        }
                    }
                }
            }

            // --- Group Size (number of members) ---
            // A scrapbook shows one photo per member at every date, so this count
            // decides the generated layout. Replaces the old template picker.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text  = "GROUP SIZE",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
                Text(
                    text  = "How many members? Each member's photo appears at every date.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTertiary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    viewModel.availableMemberCounts.forEach { count ->
                        MemberCountChip(
                            count      = count,
                            isSelected = selectedMemberCount == count,
                            onClick    = { viewModel.onMemberCountSelected(count) }
                        )
                    }
                }
            }

            // --- Journal Entry ---
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text  = "JOURNAL ENTRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
                OutlinedTextField(
                    value         = journalEntry,
                    onValueChange = viewModel::onJournalChange,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape         = RoundedCornerShape(16.dp),
                    placeholder   = {
                        Text(
                            "These moments feel like summer light through leaves...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Brown.copy(alpha = 0.5f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = Sage,
                        unfocusedBorderColor    = Beige,
                        focusedContainerColor   = Color.White.copy(alpha = 0.8f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.8f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Generate Button ---
            Button(
                onClick  = { onGenerate(selectedMemberCount) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape  = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink,
                    contentColor   = Cream
                )
            ) {
                Text("✦  Generate Scrapbook", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * What: Displays a single selectable group chip. Highlights when selected.
 *       Used for single-select group choice (like a radio button styled as a chip).
 * Who: Called by ScrapbookScreen for each available group.
 * When: Rendered in the "Select group" section.
 */
@Composable
fun GroupSelectChip(
    label:      String,
    isSelected: Boolean,
    onClick:    () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Sage.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Sage else Beige,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) AccentGreen else Brown
        )
    }
}

/**
 * What: Displays a single tag as a removable chip.
 * Who: Called by ScrapbookScreen for each tag in the tags list.
 * When: Rendered for every tag the user has added.
 */
@Composable
fun TagChip(label: String, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Sage, RoundedCornerShape(20.dp))
            .clickable { onRemove() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = AccentGreen)
    }
}

/**
 * What: Displays a dashed "+ Add tag" chip button.
 * Who: Called by ScrapbookScreen to let users add new tags.
 * When: Rendered after the existing tag chips.
 */
@Composable
fun AddTagChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Beige, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text("+ Add tag", style = MaterialTheme.typography.bodyMedium, color = Brown)
    }
}

/**
 * What: A circular selectable chip showing a group-size number. Highlights when selected.
 * Who: Called by ScrapbookScreen for each available member count.
 * When: Rendered in the "Group size" section.
 */
@Composable
fun MemberCountChip(
    count:      Int,
    isSelected: Boolean,
    onClick:    () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isSelected) Sage.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Sage else Beige,
                shape = CircleShape
            )
            .clickable { onClick() }
    ) {
        Text(
            text  = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = if (isSelected) AccentGreen else Brown
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScrapbookScreenPreview() {
    MemoryCircleTheme {
        ScrapbookScreen(groupId = "1", onBack = {})
    }
}