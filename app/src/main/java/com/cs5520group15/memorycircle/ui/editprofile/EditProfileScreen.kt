/**
 * What: Jetpack Compose UI for the Edit Profile screen.
 * Who:  Wired into the nav graph by MemoryCircleNavigation for the EditProfile
 *       route; reached from the Profile screen's "Edit Profile" action and from
 *       the Settings screen.
 * When: Composed when the user navigates to the EditProfile route.
 */

package com.cs5520group15.memorycircle.ui.editprofile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.data.NetworkUtil
import com.cs5520group15.memorycircle.model.Profile
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.Chevron
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.common.RowDivider
import com.cs5520group15.memorycircle.ui.common.SettingsRow
import com.cs5520group15.memorycircle.ui.common.brandFieldColors
import com.cs5520group15.memorycircle.ui.theme.*
import kotlinx.coroutines.launch

/**
 * What: Profile-editing form — one row per editable field (name, bio, email)
 *       plus a tappable avatar row that opens the full-size AvatarViewer.
 *       Each editable row's value is displayed inline; tapping the row pops
 *       an AlertDialog with a TextField + Save/Cancel so the user can edit
 *       and commit one field at a time.
 * Who: Called by MemoryCircleNavigation for the EditProfile route.
 * When: Reached from the "Edit Profile" button on ProfileScreen.
 *
 * @param onOpenAvatarViewer  navigates to the AvatarViewer screen
 */
@Composable
fun EditProfileScreen(
    onBack:             () -> Unit,
    onOpenAvatarViewer: () -> Unit,
    viewModel:          EditProfileViewModel = viewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    val context           = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackScope        = rememberCoroutineScope()

    /**
     * Gate every profile write through this helper. If offline, show a
     * snackbar and abort — same rationale as GroupDetail: Firestore would
     * otherwise queue the write into its offline cache and "succeed" without
     * network. WeChat / QQ also block profile edits offline; matching that.
     */
    fun runOnline(block: () -> Unit) {
        if (NetworkUtil.isOnline(context)) {
            block()
        } else {
            snackScope.launch {
                snackbarHostState.showSnackbar("No internet connection. Please try again when online.")
            }
        }
    }

    EditProfileContent(
        profile            = profile,
        snackbarHostState  = snackbarHostState,
        onBack             = onBack,
        onOpenAvatarViewer = onOpenAvatarViewer,
        onSaveName         = { runOnline { viewModel.updateName(it)  } },
        onSaveBio          = { runOnline { viewModel.updateBio(it)   } },
        onSaveEmail        = { runOnline { viewModel.updateEmail(it) } }
    )
}

/**
 * Stateless body — takes a plain Profile + callbacks so it renders in @Preview
 * without touching Firebase. EditProfileScreen above is the thin wrapper that
 * wires the ViewModel + the offline-write gate.
 */
@Composable
private fun EditProfileContent(
    profile:            Profile,
    snackbarHostState:  SnackbarHostState,
    onBack:             () -> Unit,
    onOpenAvatarViewer: () -> Unit,
    onSaveName:         (String) -> Unit,
    onSaveBio:          (String) -> Unit,
    onSaveEmail:        (String) -> Unit,
    initialEditingField: EditField? = null
) {
    var editingField by remember { mutableStateOf(initialEditingField) }

    Scaffold(
        containerColor = Cream,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            MemoryCircleTopBar(
                title    = "Profile",
                showBack = true,
                onBack   = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            AvatarRow(
                name      = profile.name,
                photoUrl  = profile.avatarUrl,
                onClick   = onOpenAvatarViewer
            )
            RowDivider()

            SettingsRow(
                label   = "Name",
                value   = profile.name,
                onClick = { editingField = EditField.NAME }
            )
            RowDivider()

            SettingsRow(
                label            = "Bio",
                value            = profile.bio.ifBlank { "Not set" },
                valuePlaceholder = profile.bio.isBlank(),
                onClick          = { editingField = EditField.BIO }
            )
            RowDivider()

            // Email is read-only for now — Firebase Auth requires a
            // re-authentication + verification flow to change the login email,
            // and we don't have email-verification configured on the project
            // yet. Rendering the row in grey so the user sees their address
            // without expecting a tap-to-edit affordance.
            SettingsRow(
                label   = "Email",
                value   = profile.email,
                enabled = false,
                onClick = {}
            )
            RowDivider()
        }
    }

    val active = editingField
    if (active != null) {
        EditFieldDialog(
            field        = active,
            initialValue = when (active) {
                EditField.NAME  -> profile.name
                EditField.BIO   -> profile.bio
                EditField.EMAIL -> profile.email
            },
            onSave = { value ->
                editingField = null
                when (active) {
                    EditField.NAME  -> onSaveName(value)
                    EditField.BIO   -> onSaveBio(value)
                    EditField.EMAIL -> onSaveEmail(value)
                }
            },
            onDismiss = { editingField = null }
        )
    }
}

/**
 * What: Which field is currently being edited via the AlertDialog. Local to
 *       this screen since it's a UI-only state machine.
 */
private enum class EditField(val label: String, val singleLine: Boolean) {
    NAME ("Name",  singleLine = true),
    BIO  ("Bio",   singleLine = false),
    EMAIL("Email", singleLine = true)
}

/**
 * What: Tappable row that displays the avatar on the right and "Avatar" label
 *       on the left. Tap opens the full-size AvatarViewer screen.
 * Who: Called by EditProfileScreen.
 * When: Rendered as the first row of the form.
 */
@Composable
private fun AvatarRow(
    name:     String,
    photoUrl: String,
    onClick:  () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp)
    ) {
        Text(
            text  = "Avatar",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Ink,
            modifier = Modifier.weight(1f)
        )
        AvatarCircle(name = name, size = 44.dp, photoUrl = photoUrl)
        Spacer(modifier = Modifier.width(8.dp))
        Chevron()
    }
}

/**
 * What: Modal AlertDialog used to edit a single profile field. Holds local
 *       draft state so changes can be cancelled cleanly. Bio is multi-line;
 *       Name and Email are single-line. Save commits to the repository via
 *       the parent's onSave callback.
 * Who: Called by EditProfileScreen when editingField is non-null.
 * When: Per row tap.
 */
@Composable
private fun EditFieldDialog(
    field:        EditField,
    initialValue: String,
    onSave:       (String) -> Unit,
    onDismiss:    () -> Unit
) {
    var draft by remember(field, initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Cream,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Text(
                text  = "Edit ${field.label.lowercase()}",
                style = MaterialTheme.typography.titleLarge,
                color = Ink
            )
        },
        text = {
            OutlinedTextField(
                value         = draft,
                onValueChange = { draft = it },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = field.singleLine,
                shape         = RoundedCornerShape(12.dp),
                colors        = brandFieldColors()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) {
                Text(
                    text  = "Save",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentGreen
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text  = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = Brown
                )
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Previews — each one targets the whole screen at a different UI state.
// ---------------------------------------------------------------------------

private val previewProfile = Profile(
    name      = "Ada Lovelace",
    bio       = "First computer programmer. Loves analytical engines & math.",
    email     = "ada@example.com",
    avatarUrl = ""
)

/** Default state — profile loaded, no dialog open. */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Edit profile · default")
@Composable
fun EditProfileScreenPreview() {
    MemoryCircleTheme {
        EditProfileContent(
            profile            = previewProfile,
            snackbarHostState  = remember { SnackbarHostState() },
            onBack             = {},
            onOpenAvatarViewer = {},
            onSaveName         = {},
            onSaveBio          = {},
            onSaveEmail        = {}
        )
    }
}

/** Empty-bio state — "Not set" placeholder rendered on the Bio row. */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Edit profile · empty bio")
@Composable
fun EditProfileScreenEmptyBioPreview() {
    MemoryCircleTheme {
        EditProfileContent(
            profile            = previewProfile.copy(bio = ""),
            snackbarHostState  = remember { SnackbarHostState() },
            onBack             = {},
            onOpenAvatarViewer = {},
            onSaveName         = {},
            onSaveBio          = {},
            onSaveEmail        = {}
        )
    }
}

/** Edit dialog open over the Name row. */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Edit profile · editing name")
@Composable
fun EditProfileScreenEditingNamePreview() {
    MemoryCircleTheme {
        EditProfileContent(
            profile             = previewProfile,
            snackbarHostState   = remember { SnackbarHostState() },
            onBack              = {},
            onOpenAvatarViewer  = {},
            onSaveName          = {},
            onSaveBio           = {},
            onSaveEmail         = {},
            initialEditingField = EditField.NAME
        )
    }
}

/** Edit dialog open over the Bio row — multi-line variant. */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Edit profile · editing bio")
@Composable
fun EditProfileScreenEditingBioPreview() {
    MemoryCircleTheme {
        EditProfileContent(
            profile             = previewProfile,
            snackbarHostState   = remember { SnackbarHostState() },
            onBack              = {},
            onOpenAvatarViewer  = {},
            onSaveName          = {},
            onSaveBio           = {},
            onSaveEmail         = {},
            initialEditingField = EditField.BIO
        )
    }
}
