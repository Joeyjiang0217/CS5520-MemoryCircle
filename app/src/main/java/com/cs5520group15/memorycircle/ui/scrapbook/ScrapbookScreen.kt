package com.cs5520group15.memorycircle.ui.scrapbook

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Placeholder for the Scrapbook creation screen.
 * Who: Called by MemoryCircleNavigation.
 * When: Navigated to when user taps a group card or the FAB on HomeScreen.
 */
@Composable
fun ScrapbookScreen(
    groupId: String,
    onBack:  () -> Unit
) {
    // Full implementation coming soon
    Text(text = "Scrapbook Screen - Group: $groupId")
}

@Preview(showBackground = true)
@Composable
fun ScrapbookScreenPreview() {
    MemoryCircleTheme {
        ScrapbookScreen(groupId = "1", onBack = {})
    }
}