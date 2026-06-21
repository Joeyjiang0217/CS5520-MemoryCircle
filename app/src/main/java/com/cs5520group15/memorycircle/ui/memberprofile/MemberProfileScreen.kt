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

    MemberProfileContent(
        state  = state,
        onBack = onBack
    )
}

/**
 * Stateless body — takes the UiState + callbacks so it renders in @Preview
 * without touching Firebase. MemberProfileScreen above is the thin wrapper
 * that wires the ViewModel.
 */
@Composable
private fun MemberProfileContent(
    state:  MemberProfileViewModel.UiState,
    onBack: () -> Unit
) {
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
        when (state) {
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
                    val m = state.info
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

// ---------------------------------------------------------------------------
// Previews — each one targets the whole screen at a different UI state.
// ---------------------------------------------------------------------------

private val previewMemberInfo = MemberProfileViewModel.MemberInfo(
    id          = "u1",
    name        = "Ada Lovelace",
    emailMasked = "a***@example.com",
    bio         = "Loves the analytical engine and analytical numbers.",
    avatarUrl   = ""
)

/** Loaded — profile fully resolved. */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Member profile · loaded")
@Composable
fun MemberProfileScreenPreview() {
    MemoryCircleTheme {
        MemberProfileContent(
            state  = MemberProfileViewModel.UiState.Loaded(previewMemberInfo),
            onBack = {}
        )
    }
}

/** Loaded with empty bio — only the name + masked email show. */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Member profile · no bio")
@Composable
fun MemberProfileScreenNoBioPreview() {
    MemoryCircleTheme {
        MemberProfileContent(
            state  = MemberProfileViewModel.UiState.Loaded(previewMemberInfo.copy(bio = "")),
            onBack = {}
        )
    }
}

/** Loading spinner — profile fetch in flight. */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Member profile · loading")
@Composable
fun MemberProfileScreenLoadingPreview() {
    MemoryCircleTheme {
        MemberProfileContent(
            state  = MemberProfileViewModel.UiState.Loading,
            onBack = {}
        )
    }
}

/** Not-found state — uid resolved to no document. */
@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE, name = "Member profile · not found")
@Composable
fun MemberProfileScreenNotFoundPreview() {
    MemoryCircleTheme {
        MemberProfileContent(
            state  = MemberProfileViewModel.UiState.NotFound,
            onBack = {}
        )
    }
}
