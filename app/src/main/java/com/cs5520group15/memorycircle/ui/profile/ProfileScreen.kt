package com.cs5520group15.memorycircle.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.MemoryCircleBottomNav
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

            OutlinedButton(
                onClick  = onOpenEditProfile,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                border   = BorderStroke(1.dp, Brown.copy(alpha = 0.5f))
            ) {
                Text(
                    text  = "Edit Profile",
                    style = MaterialTheme.typography.labelLarge,
                    color = Brown
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settings entry — icon-in-rounded-square + label + chevron, matching
            // the visual the mock uses for its Notifications / Privacy / Appearance
            // / Settings rows. Only Settings is wired up; the other three rows are
            // planned for a follow-up turn.
            SettingsEntryRow(onClick = onOpenSettings)
            HorizontalDivider(color = Beige.copy(alpha = 0.5f))
        }
    }
}

/**
 * What: One settings-list row — small rounded-square icon container on the
 *       left, label, chevron on the right. The container's Beige tint matches
 *       the icon treatment used in the mocked Notifications / Privacy /
 *       Appearance / Settings rows of the original design.
 * Who: Called by ProfileScreen.
 * When: Rendered once beneath the Edit Profile button.
 */
@Composable
private fun SettingsEntryRow(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Beige.copy(alpha = 0.4f))
        ) {
            Icon(
                painter            = painterResource(R.drawable.ic_setting),
                contentDescription = null,
                tint               = Brown,
                modifier           = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text  = "Settings",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Ink,
            modifier = Modifier.weight(1f)
        )
        Text(
            text  = "›",
            style = MaterialTheme.typography.titleLarge,
            color = InkTertiary
        )
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
