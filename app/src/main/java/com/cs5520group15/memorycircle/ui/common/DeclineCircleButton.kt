/**
 * What: Small neutral-grey round close button for declining a pending friend request.
 * Who:  Used by FriendsScreen and AllFriendRequestsScreen.
 * When: Composed next to the Accept pill in a pending friend-request row.
 */

package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.theme.GraySoft
import com.cs5520group15.memorycircle.ui.theme.InkSecondary
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Small round close button used to decline a pending friend request from
 *       the trailing slot of a request row. Neutral grey background so it reads
 *       as "dismiss" rather than "destructive" — the surface for hard-delete is
 *       a separate swipe-to-confirm action on AllFriendRequestsScreen. Backed
 *       by the vector drawable ic_close so the glyph stays crisp.
 * Who: Called by friend-request rows on FriendsScreen and AllFriendRequestsScreen.
 * When: Rendered next to the Accept pill on pending rows.
 */
@Composable
fun DeclineCircleButton(
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(GraySoft)
            .clickable { onClick() }
    ) {
        Icon(
            painter            = painterResource(id = R.drawable.ic_close),
            contentDescription = "Decline",
            tint               = InkSecondary,
            modifier           = Modifier.size(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun DeclineCircleButtonPreview() {
    MemoryCircleTheme {
        DeclineCircleButton(onClick = {})
    }
}
