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
    onGenerate: () -> Unit = {},
    viewModel: ScrapbookViewModel = viewModel()
) {
    val selectedTemplate by viewModel.selectedTemplate.collectAsStateWithLifecycle()
    val journalEntry     by viewModel.journalEntry.collectAsStateWithLifecycle()
    val tags             by viewModel.tags.collectAsStateWithLifecycle()

    var newTagInput by remember { mutableStateOf("") }
    var showAddTag  by remember { mutableStateOf(false) }

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
                // Wrap tags in a FlowRow-style layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        TagChip(
                            label     = tag,
                            onRemove  = { viewModel.onRemoveTag(tag) }
                        )
                    }
                    // Add tag chip
                    if (showAddTag) {
                        OutlinedTextField(
                            value         = newTagInput,
                            onValueChange = { newTagInput = it },
                            modifier      = Modifier.width(120.dp).height(40.dp),
                            shape         = RoundedCornerShape(20.dp),
                            singleLine    = true,
                            placeholder   = { Text("tag", style = MaterialTheme.typography.bodyMedium) },
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
                    } else {
                        AddTagChip(onClick = { showAddTag = true })
                    }
                }
            }

            // --- Preview Pages (Template Selection) ---
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text  = "PREVIEW PAGES",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("grid2", "grid4", "grid6").forEach { template ->
                        TemplatePreview(
                            template   = template,
                            isSelected = selectedTemplate == template,
                            onClick    = { viewModel.onTemplateSelected(template) },
                            modifier   = Modifier.weight(1f)
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
                onClick  = {
                    viewModel.onGenerate()
                    onGenerate()
                },
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
 * What: Displays a visual preview of a scrapbook layout template.
 *       Highlights when selected.
 * Who: Called by ScrapbookScreen for each available template option.
 * When: Rendered in the template selection row.
 */
@Composable
fun TemplatePreview(
    template:   String,
    isSelected: Boolean,
    onClick:    () -> Unit,
    modifier:   Modifier = Modifier
) {
    val gridCount = when (template) {
        "grid2" -> 2
        "grid6" -> 6
        else    -> 4
    }

    Box(
        modifier = modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Sage else Beige,
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (isSelected) Sage.copy(alpha = 0.1f) else GraySoft)
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        // Mini grid preview
        val rows = if (gridCount == 2) 1 else 2
        val cols = if (gridCount == 6) 3 else 2
        Column(
            modifier            = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(rows) {
                Row(
                    modifier              = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(cols) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Sage.copy(alpha = 0.4f) else Beige)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScrapbookScreenPreview() {
    MemoryCircleTheme {
        ScrapbookScreen(groupId = "1", onBack = {})
    }
}