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

            AvatarCircle(name = profile.name, size = 120.dp)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text  = profile.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Ink
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (profile.bio.isNotBlank()) {
                Text(
                    text      = profile.bio,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = InkSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text      = "✦ ${profile.email}",
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
        ProfileScreen(
            currentRoute      = "profile",
            onNavigate        = {},
            onOpenEditProfile = {},
            onOpenSettings    = {}
        )
    }
}
