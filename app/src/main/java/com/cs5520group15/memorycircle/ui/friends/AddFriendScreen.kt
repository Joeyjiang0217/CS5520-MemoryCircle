package com.cs5520group15.memorycircle.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
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
            TapSearchBar(onClick = onOpenSearch)

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

/**
 * What: Tap-only search bar mirroring the FriendsScreen pattern — looks like
 *       an input but the whole row is clickable so we don't pay for an active
 *       TextField on the landing screen. Placeholder text spells out the two
 *       acceptable query types (email + username) per the spec.
 * Who: Called by AddFriendScreen.
 * When: Rendered once on the landing page.
 */
@Composable
private fun TapSearchBar(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(WhiteCard)
            .border(1.dp, Beige.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Icon(
            painter            = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint               = Brown
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text  = "Email or username",
            style = MaterialTheme.typography.bodyLarge,
            color = InkTertiary
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun AddFriendScreenPreview() {
    MemoryCircleTheme {
        AddFriendScreen(onBack = {}, onOpenSearch = {})
    }
}
