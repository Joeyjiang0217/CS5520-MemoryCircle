/**
 * What: The app's shared top app bar, with an optional back button, centered title,
 *       and trailing action slot.
 * Who:  Used by most full screens, including SettingsScreen, EditProfileScreen,
 *       GroupDetailScreen, and ScrapbookScreen.
 * When: Composed at the top of a screen's Scaffold whenever that screen is shown.
 */

package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Reusable top app bar for all MemoryCircle screens.
 * Who: Called by any screen that needs a top bar (HomeScreen, ScrapbookScreen, etc.).
 * When: Rendered at the top of each screen inside a Scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryCircleTopBar(
    title: String = "MemoryCircle",
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium
            )
        },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Navigate back"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Preview(showBackground = true)
@Composable
fun MemoryCircleTopBarPreview() {
    MemoryCircleTheme {
        MemoryCircleTopBar(
            title = "New Scrapbook",
            showBack = true
        )
    }
}