/**
 * What: Centred muted text used as the empty-state placeholder for a list.
 * Who:  Used by FriendsScreen, AllFriendRequestsScreen, AddFriendSearchScreen,
 *       FriendsSearchScreen, CreateGroupScreen, and the scrapbook screens.
 * When: Composed in place of a list when there is nothing to show.
 */

package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.ui.theme.InkTertiary
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Centred body-medium hint in InkTertiary used as the empty-state for any
 *       list (no friends, no requests, no search matches, no past entries).
 *       Keeps the visual treatment consistent across screens.
 * Who: Called by lists that need a fallback when their data is empty.
 * When: Rendered in place of a list when there is nothing to show.
 */
@Composable
fun EmptyHint(
    text:     String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp)
    ) {
        Text(
            text      = text,
            style     = MaterialTheme.typography.bodyMedium,
            color     = InkTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun EmptyHintPreview() {
    MemoryCircleTheme {
        EmptyHint(text = "No friends yet.")
    }
}
