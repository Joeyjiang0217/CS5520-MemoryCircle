package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.ui.theme.Ink
import com.cs5520group15.memorycircle.ui.theme.InkTertiary
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme
import com.cs5520group15.memorycircle.ui.theme.Sage

/**
 * What: Compact list row for a group — a small Sage-tinted chip on the left, the
 *       group name above a "N members" sub-line. Replaces the two private
 *       GroupRow / GroupResultRow composables previously in FriendsScreen and
 *       FriendsSearchScreen so the visual stays consistent.
 * Who: Called by Friends and FriendsSearch screens (and any future "list of
 *      groups" surface).
 * When: Rendered for every group in a list.
 */
@Composable
fun GroupRow(
    name:        String,
    memberCount: Int,
    onClick:     () -> Unit,
    modifier:    Modifier = Modifier,
    bordered:    Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 0.dp, vertical = 10.dp)
    ) {
        val chipModifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Sage.copy(alpha = 0.7f))

        Box(
            modifier = if (bordered)
                chipModifier.border(1.dp, Sage, RoundedCornerShape(12.dp))
            else
                chipModifier
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = Ink
            )
            Text(
                text  = "$memberCount ${if (memberCount == 1) "member" else "members"}",
                style = MaterialTheme.typography.bodyMedium,
                color = InkTertiary
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun GroupRowPreview() {
    MemoryCircleTheme {
        GroupRow(
            name        = "Weekend Crew",
            memberCount = 5,
            onClick     = {},
            modifier    = Modifier.padding(horizontal = 24.dp)
        )
    }
}
