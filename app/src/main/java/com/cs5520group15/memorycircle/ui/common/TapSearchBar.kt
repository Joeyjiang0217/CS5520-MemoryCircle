/**
 * What: A non-editable, tappable search bar that navigates to a dedicated search screen when clicked.
 * Who:  Used by AddFriendScreen and FriendsScreen.
 * When: Composed as a search affordance on a list screen, opening a full search screen on tap.
 */

package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.theme.Beige
import com.cs5520group15.memorycircle.ui.theme.Brown
import com.cs5520group15.memorycircle.ui.theme.InkTertiary
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme
import com.cs5520group15.memorycircle.ui.theme.WhiteCard

/**
 * What: Tap-only search bar — looks like an OutlinedTextField but the whole row
 *       is a clickable target that navigates to a full-screen search overlay.
 *       Used wherever a screen needs a search entry point without paying for
 *       an active TextField on the landing surface.
 * Who: Called by FriendsScreen, AddFriendScreen, and any future "search entry
 *      point" landing surface.
 * When: Rendered as the search affordance below a screen's header.
 */
@Composable
fun TapSearchBar(
    placeholder: String,
    onClick:     () -> Unit,
    modifier:    Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
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
            text  = placeholder,
            style = MaterialTheme.typography.bodyLarge,
            color = InkTertiary
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun TapSearchBarPreview() {
    MemoryCircleTheme {
        TapSearchBar(placeholder = "Search friends…", onClick = {})
    }
}
