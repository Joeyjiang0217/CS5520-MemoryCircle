/**
 * What: Jetpack Compose UI for the Profile screen — the current user's profile
 *       tab landing screen.
 * Who:  Wired into the nav graph by MemoryCircleNavigation for the Profile route;
 *       reached by selecting the Profile tab in the bottom navigation bar.
 * When: Composed when the user navigates to the Profile route.
 */

package com.cs5520group15.memorycircle.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.MemoryCircleBottomNav
import com.cs5520group15.memorycircle.ui.common.RowDivider
import com.cs5520group15.memorycircle.ui.common.SecondaryOutlinedButton
import com.cs5520group15.memorycircle.ui.common.SettingsRow
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Profile tab landing screen — large centred avatar, name, bio, email,
 *       an "Edit Profile" CTA, and a Settings row beneath that drops the user
 *       into the settings hub. The original stats card is intentionally not
 *       built yet (per spec: stats / privacy / appearance rows wait for a
 *       later turn).
 * Who: Called by MemoryCircleNavigation for the Profile route.
 * When: Displayed when the user taps the Profile tab in the bottom nav.
 *
 * @param onOpenEditProfile  navigates to the EditProfile screen
 * @param onOpenSettings     navigates to the Settings hub
 */
@Composable
fun ProfileScreen(
    currentRoute:      String,
    onNavigate:        (Any) -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenSettings:    () -> Unit,
    viewModel:         ProfileViewModel = viewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    ProfileContent(
        name              = profile.name,
        bio               = profile.bio,
        email             = profile.email,
        avatarUrl         = profile.avatarUrl,
        currentRoute      = currentRoute,
        onNavigate        = onNavigate,
        onOpenEditProfile = onOpenEditProfile,
        onOpenSettings    = onOpenSettings
    )
}

/**
 * What: Stateless content of the Profile screen — renders the avatar, name, bio,
 *       email, edit CTA, and settings row purely from its parameters, so it can
 *       be shown in a @Preview without constructing a (Firebase-backed) ViewModel.
 * Who:  Used by ProfileScreen, which supplies the live ViewModel-backed profile.
 * When: Composed by ProfileScreen on every recomposition.
 */
@Composable
private fun ProfileContent(
    name:              String,
    bio:               String,
    email:             String,
    avatarUrl:         String,
    currentRoute:      String,
    onNavigate:        (Any) -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenSettings:    () -> Unit
) {
    Scaffold(
        containerColor = Cream,
        bottomBar = {
            MemoryCircleBottomNav(
                currentRoute = currentRoute,
                onNavigate   = onNavigate
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            AvatarCircle(
                name     = name,
                size     = 120.dp,
                photoUrl = avatarUrl
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text  = name,
                style = MaterialTheme.typography.headlineMedium,
                color = Ink
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bio is optional. Show a soft placeholder for brand-new users so
            // the layout doesn't visually collapse the moment they sign up.
            Text(
                text      = bio.ifBlank { "Tap Edit Profile to add a bio." },
                style     = MaterialTheme.typography.bodyMedium,
                color     = if (bio.isBlank()) InkTertiary else InkSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text      = "✦ $email",
                style     = MaterialTheme.typography.bodyMedium,
                color     = InkSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            SecondaryOutlinedButton(
                label   = "Edit Profile",
                onClick = onOpenEditProfile
            )

            Spacer(modifier = Modifier.height(24.dp))

            SettingsRow(
                label          = "Settings",
                leadingIconRes = R.drawable.ic_setting,
                onClick        = onOpenSettings
            )
            RowDivider()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun ProfileScreenPreview() {
    MemoryCircleTheme {
        ProfileContent(
            name              = "Ada Lovelace",
            bio               = "Loves hiking and old maps.",
            email             = "ada@example.com",
            avatarUrl         = "",
            currentRoute      = "profile",
            onNavigate        = {},
            onOpenEditProfile = {},
            onOpenSettings    = {}
        )
    }
}
