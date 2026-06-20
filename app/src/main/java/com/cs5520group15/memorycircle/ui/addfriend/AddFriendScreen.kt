/**
 * What: Jetpack Compose UI for the Add Friend screen — the landing page of the
 *       "add new friend" flow.
 * Who:  Wired into the nav graph by MemoryCircleNavigation for the AddFriend
 *       route; reached from FriendsScreen via its onOpenAddFriend callback.
 * When: Composed when the user navigates to the AddFriend route.
 */

package com.cs5520group15.memorycircle.ui.addfriend

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.common.TapSearchBar
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Minimal landing page for the "add new friend" flow. Holds a single
 *       tap-only search bar — tapping it opens the full-screen
 *       AddFriendSearchScreen where the active TextField and live results live.
 *       Intentionally stripped down vs. the rich WeChat-style "find people"
 *       page that inspired this screen (no contact-import shortcuts, no
 *       "people you may know" feed) — the project doesn't need them and they
 *       would mostly land as dead UI.
 * Who: Called by MemoryCircleNavigation for the AddFriend route.
 * When: Reached from the person-add icon on the Friends tab top hero.
 */
@Composable
fun AddFriendScreen(
    onBack:       () -> Unit,
    onOpenSearch: () -> Unit
) {
    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "Add new friend",
                showBack = true,
                onBack   = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            TapSearchBar(
                placeholder = "Email or username",
                onClick     = onOpenSearch
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text      = "Search by email or username to find someone.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = InkTertiary,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun AddFriendScreenPreview() {
    MemoryCircleTheme {
        AddFriendScreen(onBack = {}, onOpenSearch = {})
    }
}
