/**
 * What: Affirmative AccentGreen action pill for row trailing slots (Accept, Add).
 * Who:  Used by FriendsScreen, AllFriendRequestsScreen, and AddFriendSearchScreen.
 * When: Composed in a request/search row's trailing slot while the action is available.
 */

package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.ui.theme.AccentGreen
import com.cs5520group15.memorycircle.ui.theme.Cream
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Interactive AccentGreen pill used for affirmative actions on row
 *       trailing slots — Accept on a friend request, Add on an add-friend
 *       search result, etc. Same shape and padding everywhere it appears so
 *       these affordances read uniformly across the app.
 * Who: Called by friend-request rows and add-friend search rows.
 * When: Rendered when the action is available.
 */
@Composable
fun AcceptPill(
    label:    String,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AccentGreen)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge,
            color = Cream
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun AcceptPillPreview() {
    MemoryCircleTheme {
        AcceptPill(label = "Accept", onClick = {})
    }
}
