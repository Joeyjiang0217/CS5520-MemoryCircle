/**
 * What: Jetpack Compose UI for the Scrapbook creation screen, where a member adds a new
 *       time point or joins an existing one with a photo and description.
 * Who:  Wired into the nav graph by MemoryCircleNavigation under the ScrapbookDetail route;
 *       reached from ScrapbookViewerScreen via add-time-point and join-entry actions.
 * When: Composed when the user navigates to the ScrapbookDetail destination.
 */

package com.cs5520group15.memorycircle.ui.scrapbook

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.common.PrimaryButton
import com.cs5520group15.memorycircle.ui.common.SectionHeader
import com.cs5520group15.memorycircle.ui.common.brandFieldColors
import com.cs5520group15.memorycircle.ui.common.brandFieldColorsOnGradient
import com.cs5520group15.memorycircle.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * What: Screen for adding a memory. In new-entry mode the user sets a title, tags,
 *       picks a photo from the album and writes their own description, creating a
 *       new time point dated today. In join mode (entryId != null) the title + tags
 *       are inherited and shown read-only, and the user only adds their own photo +
 *       description to the existing time point.
 * Who: Called by MemoryCircleNavigation for the ScrapbookDetail route.
 * When: Opened from the timeline "+" FAB (new) or a card's "Add my photo" CTA (join).
 */
@Composable
fun ScrapbookScreen(
    groupId:   String,
    entryId:   String? = null,
    onBack:    () -> Unit,
    onSaved:   () -> Unit = {},
    viewModel: ScrapbookViewModel = viewModel()
) {
    LaunchedEffect(groupId, entryId) {
        viewModel.loadIfNeeded(groupId, entryId)
    }

    val title       by viewModel.title.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val tags        by viewModel.tags.collectAsStateWithLifecycle()
    val photoUri    by viewModel.selectedPhotoUri.collectAsStateWithLifecycle()
    val isSaving    by viewModel.isSaving.collectAsStateWithLifecycle()

    val isJoinMode = viewModel.isJoinMode
    val snackbarHostState = remember { SnackbarHostState() }

    // Wait for the actual save to complete (photo upload + Firestore write) before
    // navigating back, instead of fire-and-forget. Errors surface via Snackbar.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ScrapbookViewModel.SaveEvent.Success -> onSaved()
                is ScrapbookViewModel.SaveEvent.Error   -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH)) }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.onPhotoSelected(uri.toString()) }

    val onPickPhoto: () -> Unit = {
        pickPhoto.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    ScrapbookContent(
        title             = title,
        description       = description,
        tags              = tags,
        photoUri          = photoUri,
        isSaving          = isSaving,
        isJoinMode        = isJoinMode,
        canSave           = viewModel.canSave,
        today             = today,
        snackbarHostState = snackbarHostState,
        onBack            = onBack,
        onTitleChange     = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onAddTag          = viewModel::onAddTag,
        onRemoveTag       = viewModel::onRemoveTag,
        onPickPhoto       = onPickPhoto,
        onSave            = { viewModel.save(groupId, today) }
    )
}

/**
 * Stateless body — takes plain values + callbacks so it renders in @Preview
 * without touching Firebase. ScrapbookScreen above is the thin wrapper that
 * wires the ViewModel, system PhotoPicker, and event collection.
 */
@Composable
private fun ScrapbookContent(
    title:               String,
    description:         String,
    tags:                List<String>,
    photoUri:            String?,
    isSaving:            Boolean,
    isJoinMode:          Boolean,
    canSave:             Boolean,
    today:               String,
    snackbarHostState:   SnackbarHostState,
    onBack:              () -> Unit,
    onTitleChange:       (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddTag:            (String) -> Unit,
    onRemoveTag:         (String) -> Unit,
    onPickPhoto:         () -> Unit,
    onSave:              () -> Unit,
    initialShowAddTag:   Boolean = false
) {
    var newTagInput by remember { mutableStateOf("") }
    var showAddTag  by remember { mutableStateOf(initialShowAddTag) }

    val fieldColors = brandFieldColors()

    Scaffold(
        containerColor = Cream,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            MemoryCircleTopBar(
                title    = if (isJoinMode) "Add Your Photo" else "New Memory",
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

            if (!isJoinMode) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(text = "DATE")
                    Text(
                        text  = today,
                        style = MaterialTheme.typography.titleMedium,
                        color = Brown
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(text = "TITLE")
                if (isJoinMode) {
                    Text(
                        text  = title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink
                    )
                } else {
                    OutlinedTextField(
                        value         = title,
                        onValueChange = onTitleChange,
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        shape         = RoundedCornerShape(12.dp),
                        placeholder   = { Text("Name this moment", style = MaterialTheme.typography.bodyMedium) },
                        colors        = fieldColors
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(text = "TAGS")
                FlowRow(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    if (isJoinMode) {
                        if (tags.isEmpty()) {
                            Text(
                                text  = "No tags",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkTertiary
                            )
                        } else {
                            tags.forEach { ReadOnlyTagChip(it) }
                        }
                    } else {
                        tags.forEach { tag ->
                            TagChip(label = tag, onRemove = { onRemoveTag(tag) })
                        }
                        if (!showAddTag) {
                            AddTagChip(onClick = { showAddTag = true })
                        }
                    }
                }

                if (!isJoinMode && showAddTag) {
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
                            colors        = fieldColors
                        )
                        TextButton(onClick = {
                            onAddTag(newTagInput)
                            newTagInput = ""
                            showAddTag  = false
                        }) {
                            Text("Add", color = AccentGreen)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(text = "YOUR PHOTO")
                if (photoUri == null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, Beige, RoundedCornerShape(16.dp))
                            .clickable { onPickPhoto() }
                    ) {
                        Text("📷  Choose from album", color = Brown, style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    AsyncImage(
                        model              = photoUri,
                        contentDescription = "Selected photo",
                        contentScale       = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPickPhoto() }
                    )
                    TextButton(onClick = onPickPhoto) {
                        Text("Change photo", color = AccentGreen)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(text = "YOUR DESCRIPTION")
                OutlinedTextField(
                    value         = description,
                    onValueChange = onDescriptionChange,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape         = RoundedCornerShape(16.dp),
                    placeholder   = {
                        Text(
                            "Say something about your photo...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Brown.copy(alpha = 0.5f)
                        )
                    },
                    colors = brandFieldColorsOnGradient()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            PrimaryButton(
                label   = if (isJoinMode) "✦  Add to Timeline" else "✦  Create Memory",
                onClick = onSave,
                enabled = canSave && !isSaving,
                loading = isSaving
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * What: Displays a single tag as a removable chip.
 * Who: Called by ScrapbookContent for each editable tag.
 */
@Composable
private fun TagChip(label: String, onRemove: () -> Unit) {
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
 * What: Displays an inherited tag as a non-removable chip (join mode).
 * Who: Called by ScrapbookContent when showing a creator's tags read-only.
 */
@Composable
private fun ReadOnlyTagChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Beige, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Brown)
    }
}

/**
 * What: Displays a dashed "+ Add tag" chip button.
 * Who: Called by ScrapbookContent to let users add new tags.
 */
@Composable
private fun AddTagChip(onClick: () -> Unit) {
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

// ---------------------------------------------------------------------------
// Previews — each one targets the whole screen at a different UI state.
// ---------------------------------------------------------------------------

/** Default new-entry mode — empty form, photo placeholder visible. */
@Preview(showBackground = true, name = "Scrapbook · new · empty")
@Composable
fun ScrapbookScreenPreview() {
    MemoryCircleTheme {
        ScrapbookContent(
            title               = "",
            description         = "",
            tags                = emptyList(),
            photoUri            = null,
            isSaving            = false,
            isJoinMode          = false,
            canSave             = false,
            today               = "August 21",
            snackbarHostState   = remember { SnackbarHostState() },
            onBack              = {},
            onTitleChange       = {},
            onDescriptionChange = {},
            onAddTag            = {},
            onRemoveTag         = {},
            onPickPhoto         = {},
            onSave              = {}
        )
    }
}

/** New-entry mode with content filled — title, tags, description set. */
@Preview(showBackground = true, name = "Scrapbook · new · filled")
@Composable
fun ScrapbookScreenFilledPreview() {
    MemoryCircleTheme {
        ScrapbookContent(
            title               = "Lakeside Picnic",
            description         = "Beautiful sunset over the water with the whole gang.",
            tags                = listOf("food", "park", "summer"),
            photoUri            = null,
            isSaving            = false,
            isJoinMode          = false,
            canSave             = true,
            today               = "August 21",
            snackbarHostState   = remember { SnackbarHostState() },
            onBack              = {},
            onTitleChange       = {},
            onDescriptionChange = {},
            onAddTag            = {},
            onRemoveTag         = {},
            onPickPhoto         = {},
            onSave              = {}
        )
    }
}

/** Add-tag input visible — user tapped the "+ Add tag" chip. */
@Preview(showBackground = true, name = "Scrapbook · new · adding tag")
@Composable
fun ScrapbookScreenAddingTagPreview() {
    MemoryCircleTheme {
        ScrapbookContent(
            title               = "Lakeside Picnic",
            description         = "",
            tags                = listOf("food"),
            photoUri            = null,
            isSaving            = false,
            isJoinMode          = false,
            canSave             = false,
            today               = "August 21",
            snackbarHostState   = remember { SnackbarHostState() },
            onBack              = {},
            onTitleChange       = {},
            onDescriptionChange = {},
            onAddTag            = {},
            onRemoveTag         = {},
            onPickPhoto         = {},
            onSave              = {},
            initialShowAddTag   = true
        )
    }
}

/** Join mode — title + tags inherited and shown read-only. */
@Preview(showBackground = true, name = "Scrapbook · join mode")
@Composable
fun ScrapbookScreenJoinModePreview() {
    MemoryCircleTheme {
        ScrapbookContent(
            title               = "Lakeside Picnic",
            description         = "",
            tags                = listOf("food", "park"),
            photoUri            = null,
            isSaving            = false,
            isJoinMode          = true,
            canSave             = false,
            today               = "August 21",
            snackbarHostState   = remember { SnackbarHostState() },
            onBack              = {},
            onTitleChange       = {},
            onDescriptionChange = {},
            onAddTag            = {},
            onRemoveTag         = {},
            onPickPhoto         = {},
            onSave              = {}
        )
    }
}

/** Saving spinner — save in flight. */
@Preview(showBackground = true, name = "Scrapbook · saving")
@Composable
fun ScrapbookScreenSavingPreview() {
    MemoryCircleTheme {
        ScrapbookContent(
            title               = "Lakeside Picnic",
            description         = "Sunset photos",
            tags                = listOf("food", "park"),
            photoUri            = null,
            isSaving            = true,
            isJoinMode          = false,
            canSave             = true,
            today               = "August 21",
            snackbarHostState   = remember { SnackbarHostState() },
            onBack              = {},
            onTitleChange       = {},
            onDescriptionChange = {},
            onAddTag            = {},
            onRemoveTag         = {},
            onPickPhoto         = {},
            onSave              = {}
        )
    }
}
