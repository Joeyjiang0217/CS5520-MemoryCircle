/**
 * What: Jetpack Compose UI for the Member Profile screen.
 * Who:  Wired into the nav graph by MemoryCircleNavigation for the MemberProfile
 *       route; reached from GroupMembers, the Friends screen, AddFriendSearch,
 *       and FriendsSearch when opening another user's profile.
 * When: Composed when the user navigates to the MemberProfile route.
 */

package com.cs5520group15.memorycircle.ui.memberprofile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.EmptyHint
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Read-only profile view for someone OTHER than the current user. Layout
 *       mirrors ProfileScreen's hero — large centred avatar, name, optional
 *       bio sub-line, masked email — but omits the Edit-Profile / Settings
 *       affordances since those only make sense for the current user.
 *
 *       The email shown here is the `emailMasked` field on users/{uid} (e.g.
 *       "1***@test.com"); the unmasked address never leaves Firebase Auth,
 *       so no client-side masking is needed at this layer.
 * Who: Called by MemoryCircleNavigation for the MemberProfile route.
 * When: Reached by tapping a friend row, search result, request row, group
 *       member, or thumbnail anywhere in the app.
 */
@Composable
fun MemberProfileScreen(
    userId:    String,
    onBack:    () -> Unit,
    viewModel: MemberProfileViewModel = viewModel()
) {
    LaunchedEffect(userId) { viewModel.bind(userId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "",
                showBack = true,
                onBack   = onBack
            )
        }
    ) { padding ->
        when (val s = state) {
            MemberProfileViewModel.UiState.Loading -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Brown)
                }
            }
            MemberProfileViewModel.UiState.NotFound -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    EmptyHint(text = "We couldn't find that user.")
                }
            }
            is MemberProfileViewModel.UiState.Loaded -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val m = s.info
                    Spacer(modifier = Modifier.height(40.dp))

                    AvatarCircle(
                        name     = m.name,
                        size     = 120.dp,
                        photoUrl = m.avatarUrl
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text  = m.name.ifBlank { "User" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = Ink
                    )

                    if (m.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text      = m.bio,
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = InkSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (m.emailMasked.isNotBlank()) {
                        Text(
                            text      = "✦ ${m.emailMasked}",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = InkSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun MemberProfileScreenPreview() {
    MemoryCircleTheme {
        MemberProfileScreen(userId = "u_emma", onBack = {})
    }
}
