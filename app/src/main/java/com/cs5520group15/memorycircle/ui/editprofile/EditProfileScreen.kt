package com.cs5520group15.memorycircle.ui.editprofile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.Chevron
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.common.RowDivider
import com.cs5520group15.memorycircle.ui.common.SettingsRow
import com.cs5520group15.memorycircle.ui.common.brandFieldColors
import com.cs5520group15.memorycircle.ui.theme.*

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

    var editingField by remember { mutableStateOf<EditField?>(null) }

    Scaffold(
        containerColor = Cream,
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
                name    = profile.name,
                onClick = onOpenAvatarViewer
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

            SettingsRow(
                label   = "Email",
                value   = profile.email,
                onClick = { editingField = EditField.EMAIL }
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
                when (active) {
                    EditField.NAME  -> viewModel.updateName(value)
                    EditField.BIO   -> viewModel.updateBio(value)
                    EditField.EMAIL -> viewModel.updateEmail(value)
                }
                editingField = null
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
    name:    String,
    onClick: () -> Unit
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
        AvatarCircle(name = name, size = 44.dp)
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

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun EditProfileScreenPreview() {
    MemoryCircleTheme {
        EditProfileScreen(
            onBack             = {},
            onOpenAvatarViewer = {}
        )
    }
}
