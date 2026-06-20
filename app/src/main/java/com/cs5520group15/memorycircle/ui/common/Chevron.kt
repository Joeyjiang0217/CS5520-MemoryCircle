/**
 * What: Right-pointing chevron glyph hinting that a row drills in on tap.
 * Who:  Used by EditProfileScreen rows (and any tappable navigation row).
 * When: Composed as a trailing affordance at the end of a tappable row.
 */

package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.theme.InkTertiary
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: A right-pointing chevron used as a "tap to drill in" hint at the end of
 *       tappable rows (Profile, Settings, EditProfile, scrapbook month rows).
 *       Backed by the vector drawable ic_chevron_right so the glyph stays
 *       crisp at every density and inherits the row's tint color cleanly.
 * Who: Called by any row that navigates somewhere on tap.
 * When: Rendered as a trailing affordance.
 */
@Composable
fun Chevron(color: Color = InkTertiary) {
    Icon(
        painter            = painterResource(id = R.drawable.ic_chevron_right),
        contentDescription = null,
        tint               = color,
        modifier           = Modifier.size(20.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun ChevronPreview() {
    MemoryCircleTheme {
        Chevron()
    }
}
