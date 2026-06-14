package com.cs5520group15.memorycircle.ui.friends

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
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Read-only profile view for someone OTHER than the current user. Layout
 *       mirrors ProfileScreen's hero — large centred avatar, name, sub-line,
 *       email — but omits the Edit-Profile / Settings affordances since those
 *       only make sense for the current user. The email is partially masked
 *       (first two local-part characters preserved, rest replaced with four
 *       stars) for privacy: "sarah.chen@gmail.com" → "sa****@gmail.com".
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
    val member by viewModel.member.collectAsStateWithLifecycle()

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
        val m = member
        if (m == null) {
            // Hit when the user navigates to their own id (the current user
            // isn't seeded into the discoverable pool), or when the seed pool
            // changes after this id was captured. Friendly empty state — no
            // ugly crash, no half-rendered hero.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text      = "We couldn't find that user.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = InkTertiary,
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            AvatarCircle(name = m.name, size = 120.dp)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text  = m.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Ink
            )

            // Shared-memory count sits in the same vertical slot as the bio on
            // ProfileScreen — only meaningful when this is actually a friend.
            if (m.sharedMemories > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text      = "${m.sharedMemories} shared memories",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = InkSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text      = "✦ ${maskEmail(m.email)}",
                style     = MaterialTheme.typography.bodyMedium,
                color     = InkSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * What: Masks the middle of an email's local part for privacy. Keeps the
 *       first two characters (or one if the local part is shorter), drops
 *       four stars after them, then preserves the @ and domain verbatim.
 *       "sarah.chen@gmail.com" → "sa****@gmail.com".
 *       "a@x.com"               → "a****@x.com".
 *       Strings without a "@" return unchanged — defensive against
 *       malformed input.
 * Who: Called by MemberProfileScreen.
 * When: Per render.
 */
private fun maskEmail(email: String): String {
    val at = email.indexOf('@')
    if (at <= 0) return email
    val local   = email.substring(0, at)
    val domain  = email.substring(at)
    val visible = local.take(if (local.length >= 2) 2 else 1)
    return "$visible****$domain"
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun MemberProfileScreenPreview() {
    MemoryCircleTheme {
        MemberProfileScreen(userId = "u_emma", onBack = {})
    }
}
