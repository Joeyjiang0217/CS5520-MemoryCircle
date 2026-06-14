package com.cs5520group15.memorycircle.ui.avatarviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.profile.ProfileViewModel
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Full-size avatar viewer reached by tapping the avatar row on
 *       EditProfile. Top bar carries a back button and an "more" icon on the
 *       right; tapping the icon surfaces an action menu (pick from album /
 *       save image / cancel). Album-pick and save-image are placeholder
 *       callbacks for now — the actual photo-picker / file-write wiring
 *       lands when the profile picture moves from a letter avatar to a real
 *       URI-backed image.
 * Who: Called by MemoryCircleNavigation for the AvatarViewer route.
 * When: Reached from the Avatar row on EditProfileScreen.
 */
@Composable
fun AvatarViewerScreen(
    onBack:    () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    var showActions by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "Profile Picture",
                showBack = true,
                onBack   = onBack,
                actions  = {
                    IconButton(onClick = { showActions = true }) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_more),
                            contentDescription = "More options",
                            tint               = Ink
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Letter avatar blown up. Once a real avatar URI is in profile,
            // swap this for an AsyncImage anchored to that URI.
            AvatarCircle(name = profile.name, size = 240.dp)
        }
    }

    if (showActions) {
        AvatarActionMenu(
            onPickFromAlbum = {
                // TODO: launch ActivityResultContracts.PickVisualMedia and
                //       commit the resulting URI to ProfileRepository.
                showActions = false
            },
            onSaveImage = {
                // TODO: render the avatar to a Bitmap and persist it via
                //       MediaStore. Letter avatar is purely decorative for
                //       now, so this is a no-op until a real image lands.
                showActions = false
            },
            onCancel = { showActions = false }
        )
    }
}

/**
 * What: Three-item action menu rendered as a centred Dialog (stacked rows
 *       separated by Beige dividers) rather than a ModalBottomSheet to keep
 *       the look tied to the brand's Cream + serif palette.
 * Who: Called by AvatarViewerScreen when the more-options icon is tapped.
 * When: Visible while showActions is true on the parent.
 */
@Composable
private fun AvatarActionMenu(
    onPickFromAlbum: () -> Unit,
    onSaveImage:     () -> Unit,
    onCancel:        () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .background(Cream, RoundedCornerShape(20.dp))
                .padding(vertical = 4.dp)
        ) {
            ActionRow(label = "Choose from album", onClick = onPickFromAlbum)
            HorizontalDivider(color = Beige.copy(alpha = 0.5f))
            ActionRow(label = "Save image",        onClick = onSaveImage)
            HorizontalDivider(color = Beige.copy(alpha = 0.5f))
            ActionRow(label = "Cancel",            onClick = onCancel, emphasis = true)
        }
    }
}

/**
 * What: One action row in the avatar menu. Cancel is rendered with Brown
 *       emphasis so the user sees the dismiss option clearly.
 * Who: Called by AvatarActionMenu.
 * When: Rendered for each action.
 */
@Composable
private fun ActionRow(
    label:    String,
    onClick:  () -> Unit,
    emphasis: Boolean = false
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 24.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (emphasis) Brown else Ink
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun AvatarViewerScreenPreview() {
    MemoryCircleTheme {
        AvatarViewerScreen(onBack = {})
    }
}
