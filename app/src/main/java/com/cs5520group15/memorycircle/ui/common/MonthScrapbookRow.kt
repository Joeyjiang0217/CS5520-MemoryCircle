package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cs5520group15.memorycircle.ui.theme.Beige
import com.cs5520group15.memorycircle.ui.theme.Brown
import com.cs5520group15.memorycircle.ui.theme.Ink
import com.cs5520group15.memorycircle.ui.theme.InkTertiary
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme
import com.cs5520group15.memorycircle.ui.theme.Sage
import com.cs5520group15.memorycircle.ui.theme.WhiteCard

/**
 * What: One row in a per-month scrapbook list — leading chip on the left, the
 *       title above a "📷 N memories" sub-line, chevron on the right. The
 *       leading chip renders a real thumbnail (Coil AsyncImage cropped into
 *       the rounded square) when `thumbnailUrl` is non-blank, and falls back
 *       to the solid color block when the scrapbook has no posts yet.
 *       Replaces the byte-identical MonthScrapbookRow (GroupDetail) and
 *       ScrapbookCard (MemoriesScreen) helpers.
 * Who: Called by GroupDetailScreen and MemoriesScreen.
 * When: Rendered for every month bucket in either surface.
 */
@Composable
fun MonthScrapbookRow(
    title:        String,
    memoryCount:  Int,
    colorType:    String,
    onClick:      () -> Unit,
    modifier:     Modifier = Modifier,
    thumbnailUrl: String?  = null
) {
    val accent = if (colorType == "sage") Sage else Brown

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WhiteCard)
            .border(1.dp, Beige.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.85f))
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model              = thumbnailUrl,
                    contentDescription = "Latest memory preview",
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            }
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.titleLarge,
                color = Ink
            )
            Text(
                text  = "📷 $memoryCount memories",
                style = MaterialTheme.typography.bodyMedium,
                color = InkTertiary
            )
        }
        Chevron(color = Brown)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun MonthScrapbookRowPreview() {
    MemoryCircleTheme {
        MonthScrapbookRow(
            title       = "March 2025",
            memoryCount = 9,
            colorType   = "brown",
            onClick     = {},
            modifier    = Modifier.padding(24.dp)
        )
    }
}
