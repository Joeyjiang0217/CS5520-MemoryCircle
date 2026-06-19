package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.ui.theme.AccentGreen
import com.cs5520group15.memorycircle.ui.theme.Ink
import com.cs5520group15.memorycircle.ui.theme.InkTertiary
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Generic "avatar + name + subtitle + optional trailing" row used across
 *       the friends/groups surfaces. Replaces five near-identical private
 *       composables that previously lived inside FriendsScreen, GroupMembersScreen,
 *       AllFriendRequestsScreen, AddFriendSearchScreen, and FriendsSearchScreen.
 *       The trailing slot accepts anything (Accept/Decline pills, "Active" chip,
 *       a count, nothing) so each caller composes its own affordances without
 *       this component growing knobs for every variant.
 * Who: Called by any list rendering a person row with avatar + label.
 * When: Rendered for each person in the list.
 */
@Composable
fun AvatarListRow(
    name:          String,
    subtitle:      String,
    onClick:       () -> Unit,
    modifier:      Modifier = Modifier,
    isOnline:      Boolean = false,
    avatarSize:    Dp = 44.dp,
    rowVerticalPadding: Dp = 10.dp,
    photoUrl:      String? = null,
    trailing:      @Composable RowScope.() -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = rowVerticalPadding)
    ) {
        Box {
            AvatarCircle(name = name, size = avatarSize, photoUrl = photoUrl)
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size((avatarSize.value * 0.25f).coerceAtLeast(10f).dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = name,
                style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color    = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text     = subtitle,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = InkTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        trailing()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun AvatarListRowPreview() {
    MemoryCircleTheme {
        AvatarListRow(
            name     = "Emma Wilson",
            subtitle = "34 shared memories",
            isOnline = true,
            onClick  = {}
        )
    }
}
