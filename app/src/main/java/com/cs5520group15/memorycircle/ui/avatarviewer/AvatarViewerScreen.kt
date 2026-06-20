/**
 * What: Jetpack Compose UI for the Avatar Viewer screen.
 * Who:  Wired into the nav graph by MemoryCircleNavigation for the AvatarViewer
 *       route; reached from the Edit Profile screen when opening the avatar.
 * When: Composed when the user navigates to the AvatarViewer route.
 */

package com.cs5520group15.memorycircle.ui.avatarviewer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.data.NetworkUtil
import com.cs5520group15.memorycircle.data.ProfileRepository
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.profile.ProfileViewModel
import com.cs5520group15.memorycircle.ui.theme.*
import kotlinx.coroutines.launch

/**
 * What: Full-size avatar viewer reached by tapping the avatar row on
 *       EditProfile. Top bar carries a back button and an "more" icon on the
 *       right; tapping the icon surfaces an action menu (pick from album /
 *       cancel). Picking an image launches the system PhotoPicker, then
 *       ProfileRepository uploads the result to Firebase Storage and patches
 *       users/{uid}.avatarUrl — the live profile listener pushes the new URL
 *       back to every screen showing this user's avatar.
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
    var uploading   by remember { mutableStateOf(false) }
    var errorText   by remember { mutableStateOf<String?>(null) }

    val scope             = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context           = LocalContext.current

    // System PhotoPicker — no extra runtime permission needed on Android 13+.
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        errorText = null
        scope.launch {
            runCatching { ProfileRepository.uploadAvatar(uri.toString()) }
                .onFailure { errorText = it.message ?: "Upload failed" }
            uploading = false
        }
    }

    LaunchedEffect(errorText) {
        errorText?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = Cream,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
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
            AvatarCircle(
                name     = profile.name,
                size     = 240.dp,
                photoUrl = profile.avatarUrl
            )
            if (uploading) {
                CircularProgressIndicator(color = Brown)
            }
        }
    }

    if (showActions) {
        AvatarActionMenu(
            onPickFromAlbum = {
                showActions = false
                // Gate the photo picker on connectivity so the user doesn't
                // pick + crop only to have the upload silently queue into
                // Firestore's offline cache. Matches WeChat / QQ — avatar
                // edits are blocked offline.
                if (NetworkUtil.isOnline(context)) {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("No internet connection. Please try again when online.")
                    }
                }
            },
            onCancel = { showActions = false }
        )
    }
}

/**
 * What: Action menu rendered as a centred Dialog (stacked rows separated by
 *       Beige dividers) rather than a ModalBottomSheet to keep the look tied
 *       to the brand's Cream + serif palette. "Save image" has been dropped
 *       since the avatar lives in Firestore now — there's nothing local to
 *       export until we ever ship a "save the bytes of this remote image"
 *       feature.
 * Who: Called by AvatarViewerScreen when the more-options icon is tapped.
 * When: Visible while showActions is true on the parent.
 */
@Composable
private fun AvatarActionMenu(
    onPickFromAlbum: () -> Unit,
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
            ActionRow(label = "Cancel",            onClick = onCancel, emphasis = true)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun AvatarActionMenuPreview() {
    MemoryCircleTheme {
        AvatarActionMenu(
            onPickFromAlbum = {},
            onCancel        = {}
        )
    }
}

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
fun ActionRowPreview() {
    MemoryCircleTheme {
        ActionRow(
            label   = "Choose from album",
            onClick = {}
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
