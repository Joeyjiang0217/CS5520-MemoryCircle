package com.cs5520group15.memorycircle.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Placeholder for the "create a new group" flow, where a user will pick
 *       contacts to form a new group (a circle of people).
 *       The real contact-picker / group-creation UI is owned by a teammate;
 *       this stub only exists so the Home "+" button has a destination to open
 *       and the app builds and runs.
 * Who: Called by MemoryCircleNavigation when the user taps the "+" FAB on HomeScreen.
 * When: Displayed when navigating to the CreateGroup route.
 */
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "New Group",
                showBack = true,
                onBack   = onBack
            )
        }
    ) { padding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text      = "Select contacts to create a group",
                    style     = MaterialTheme.typography.titleLarge,
                    color     = Ink,
                    textAlign = TextAlign.Center
                )
                Text(
                    text      = "Coming soon — this is where you'll pick people to add to a new group.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = InkTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateGroupScreenPreview() {
    MemoryCircleTheme {
        CreateGroupScreen(onBack = {})
    }
}
