package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cs5520group15.memorycircle.ui.theme.Ink
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Small uppercase section label, optionally paired with trailing content
 *       on the right (a "See all" link, a count, etc.). Used wherever the app
 *       introduces a stacked list with a tiny header — Home's RECENT GROUPS,
 *       Friends' FRIEND REQUESTS, GroupDetail's MEMBERS / SCRAPBOOKS, the
 *       scrapbook creation form's DATE / TITLE / TAGS / YOUR PHOTO sections,
 *       and so on.
 * Who: Called by any screen that introduces a labelled section.
 * When: Rendered above the section's content.
 */
@Composable
fun SectionHeader(
    text:     String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.labelSmall,
            color = Ink
        )
        trailing()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun SectionHeaderPreview() {
    MemoryCircleTheme {
        SectionHeader(text = "RECENT GROUPS")
    }
}
