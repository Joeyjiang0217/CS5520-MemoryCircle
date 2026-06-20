/**
 * What: Compact group list row with a WeChat-style avatar-collage chip, name, and member count.
 * Who:  Used by FriendsScreen and FriendsSearchScreen.
 * When: Composed for each group in a list of groups.
 */

package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cs5520group15.memorycircle.ui.theme.Beige
import com.cs5520group15.memorycircle.ui.theme.Cream
import com.cs5520group15.memorycircle.ui.theme.Ink
import com.cs5520group15.memorycircle.ui.theme.InkTertiary
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme
import com.cs5520group15.memorycircle.ui.theme.Sage

/**
 * What: Compact list row for a group — a small chip on the left, the group
 *       name above a "N members" sub-line.
 *
 *       The chip renders a WeChat-style avatar collage when memberAvatarUrls
 *       / memberNames are non-empty: small filled cells laid out in a
 *       grid (1 / 2 / 1+2 / 2×2 / 2+3 / 2×3 / 3×3) following WeChat's group
 *       avatar conventions, up to 9 entries. Each cell fills with the
 *       member's avatar image when known, or a Sage-tinted letter when not.
 *       When no member data is provided the chip falls back to the original
 *       Sage solid square — used by search results that don't have member
 *       data in hand.
 *
 * Who: Called by Friends and FriendsSearch screens (and any future "list of
 *      groups" surface).
 * When: Rendered for every group in a list.
 */
@Composable
fun GroupRow(
    name:             String,
    memberCount:      Int,
    onClick:          () -> Unit,
    modifier:         Modifier = Modifier,
    bordered:         Boolean  = false,
    memberAvatarUrls: List<String> = emptyList(),
    memberNames:      List<String> = emptyList()
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 0.dp, vertical = 10.dp)
    ) {
        if (memberNames.any { it.isNotBlank() } || memberAvatarUrls.any { it.isNotBlank() }) {
            AvatarCollageChip(
                avatarUrls = memberAvatarUrls,
                names      = memberNames
            )
        } else {
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
        }
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

/**
 * 44dp rounded-square chip holding up to 9 member avatars. The cells are
 * rectangular (not circular) so they tile cleanly inside the chip; clipping
 * each cell to a circle at this scale leaves ugly visible gaps between
 * neighbours. Empty trailing cells stay Beige so the chip outline reads
 * cleanly when memberCount < 9.
 */
@Composable
private fun AvatarCollageChip(
    avatarUrls: List<String>,
    names:      List<String>
) {
    val count = minOf(maxOf(avatarUrls.size, names.size), 9)
    val pairs = (0 until count).map { idx ->
        (names.getOrNull(idx).orEmpty()) to (avatarUrls.getOrNull(idx).orEmpty())
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Beige.copy(alpha = 0.5f))
    ) {
        when (count) {
            0    -> Unit
            1    -> CollageRow(listOf(pairs[0]), Modifier.fillMaxSize())
            2    -> CollageRow(pairs, Modifier.fillMaxSize())
            3    -> Column(modifier = Modifier.fillMaxSize()) {
                CollageRow(listOf(pairs[0]),                Modifier.weight(1f))
                CollageRow(listOf(pairs[1], pairs[2]),      Modifier.weight(1f))
            }
            4    -> Column(modifier = Modifier.fillMaxSize()) {
                CollageRow(listOf(pairs[0], pairs[1]), Modifier.weight(1f))
                CollageRow(listOf(pairs[2], pairs[3]), Modifier.weight(1f))
            }
            5    -> Column(modifier = Modifier.fillMaxSize()) {
                CollageRow(listOf(pairs[0], pairs[1]),              Modifier.weight(1f))
                CollageRow(listOf(pairs[2], pairs[3], pairs[4]),    Modifier.weight(1f))
            }
            6    -> Column(modifier = Modifier.fillMaxSize()) {
                CollageRow(listOf(pairs[0], pairs[1], pairs[2]), Modifier.weight(1f))
                CollageRow(listOf(pairs[3], pairs[4], pairs[5]), Modifier.weight(1f))
            }
            else -> Column(modifier = Modifier.fillMaxSize()) {
                CollageRow(pairs.subList(0, 3),                            Modifier.weight(1f))
                CollageRow(pairs.subList(3, minOf(6, count)).padTo(3),     Modifier.weight(1f))
                CollageRow(pairs.subList(minOf(6, count), count).padTo(3), Modifier.weight(1f))
            }
        }
    }
}

private fun List<Pair<String, String>>.padTo(size: Int): List<Pair<String, String>> =
    this + List((size - this.size).coerceAtLeast(0)) { "" to "" }

@Composable
private fun CollageRow(
    cells:    List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        cells.forEachIndexed { _, (name, url) ->
            CollageCell(
                name     = name,
                photoUrl = url,
                modifier = Modifier.weight(1f).fillMaxSize()
            )
        }
    }
}

@Composable
private fun CollageCell(
    name:     String,
    photoUrl: String,
    modifier: Modifier = Modifier
) {
    if (name.isBlank() && photoUrl.isBlank()) {
        Box(modifier = modifier)   // empty Beige cell
        return
    }
    if (photoUrl.isNotBlank()) {
        AsyncImage(
            model              = photoUrl,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = modifier.background(Sage)
        )
        return
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier.background(Sage)
    ) {
        Text(
            text     = name.firstOrNull()?.uppercaseChar()?.toString() ?: "",
            color    = Cream,
            fontSize = 9.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun GroupRowPreview() {
    MemoryCircleTheme {
        GroupRow(
            name             = "Weekend Crew",
            memberCount      = 5,
            onClick          = {},
            modifier         = Modifier.padding(horizontal = 24.dp),
            memberNames      = listOf("Alice", "Bob", "Cara", "Dan", "Eve"),
            memberAvatarUrls = listOf("", "", "", "", "")
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun AvatarCollageChipPreview() {
    MemoryCircleTheme {
        AvatarCollageChip(
            avatarUrls = listOf("", "", "", "", ""),
            names      = listOf("Alice", "Bob", "Cara", "Dan", "Eve")
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun CollageRowPreview() {
    MemoryCircleTheme {
        CollageRow(
            cells = listOf("Alice" to "", "Bob" to "", "Cara" to "")
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun CollageCellPreview() {
    MemoryCircleTheme {
        CollageCell(
            name     = "Alice",
            photoUrl = ""
        )
    }
}
