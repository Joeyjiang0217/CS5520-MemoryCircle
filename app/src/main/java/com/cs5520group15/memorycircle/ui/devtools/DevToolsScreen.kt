package com.cs5520group15.memorycircle.ui.devtools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.common.PrimaryButton
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Debug-only utility page. Three buttons run the SeedRepository
 *       actions while keeping the developer signed in. Each row shows its
 *       own running spinner / success message / error text so the user can
 *       see what worked without leaving the screen.
 * Who: Called by MemoryCircleNavigation for the DevTools route, reached from
 *      the "Dev Tools" row on Settings.
 */
@Composable
fun DevToolsScreen(
    onBack:    () -> Unit,
    viewModel: DevToolsViewModel = viewModel()
) {
    val appContext = LocalContext.current.applicationContext

    val usersState         by viewModel.usersState.collectAsStateWithLifecycle()
    val postState          by viewModel.postState.collectAsStateWithLifecycle()
    val historyState       by viewModel.historyState.collectAsStateWithLifecycle()
    val friendsState       by viewModel.friendsState.collectAsStateWithLifecycle()
    val profilesState      by viewModel.profilesState.collectAsStateWithLifecycle()
    val clearProfilesState by viewModel.clearProfilesState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = "Dev Tools",
                showBack = true,
                onBack   = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text  = "DEBUG SEED ACTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = Ink
            )

            SeedSection(
                title       = "Register 10 test users",
                description = "1@test.com … 10@test.com, password \"123456\". Uses a separate Firebase app instance so YOUR session stays signed in. Safe to re-run — duplicates are skipped.",
                buttonLabel = "Seed users",
                state       = usersState,
                onRun       = { viewModel.seedUsers(appContext) }
            )

            SeedSection(
                title       = "Add a test post to current month",
                description = "Inserts one seeded post into the current-month scrapbook of YOUR first group. Group must already exist (use Home → + to create one).",
                buttonLabel = "Seed test post",
                state       = postState,
                onRun       = { viewModel.seedPost() }
            )

            SeedSection(
                title       = "Add 3 historical scrapbooks",
                description = "Creates the past three months of scrapbooks for your first group, each with two seeded posts. Skips months that already have a scrapbook doc.",
                buttonLabel = "Seed history",
                state       = historyState,
                onRun       = { viewModel.seedHistory() }
            )

            SeedSection(
                title       = "Befriend 1-5@test.com",
                description = "Adds 1@test.com through 5@test.com to YOUR friend list (writes both sides of the relationship). 6-10@test.com are intentionally left out so you can test the add-friend flow against them.",
                buttonLabel = "Seed friendships",
                state       = friendsState,
                onRun       = { viewModel.seedFriendships() }
            )

            SeedSection(
                title       = "Decorate test users",
                description = "Patches a themed bio + a stable picsum avatar URL onto every test user (1-10@test.com) so member lists, group avatar collages, and friend rows don't all show the same Sage letter. Also upgrades any old-shape doc to the new public shape (emailMasked).",
                buttonLabel = "Seed profiles",
                state       = profilesState,
                onRun       = { viewModel.seedProfiles() }
            )

            SeedSection(
                title       = "Strip test-user profiles",
                description = "Blanks the bio + avatar on every test user (1-10@test.com) so the demo can show the fresh / undecorated state, or to verify cross-device avatar refresh by running clear → seed and watching every screen update.",
                buttonLabel = "Clear profiles",
                state       = clearProfilesState,
                onRun       = { viewModel.clearProfiles() }
            )
        }
    }
}

@Composable
private fun SeedSection(
    title:       String,
    description: String,
    buttonLabel: String,
    state:       DevToolsViewModel.ActionState,
    onRun:       () -> Unit
) {
    val running = state is DevToolsViewModel.ActionState.Running
    Card(
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = WhiteCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = title,       style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(text = description, style = MaterialTheme.typography.bodyMedium,  color = InkSecondary)
            PrimaryButton(
                label   = if (running) "Running…" else buttonLabel,
                onClick = onRun,
                loading = running
            )
            ResultLine(state)
        }
    }
}

@Composable
private fun ResultLine(state: DevToolsViewModel.ActionState) {
    val (text, color) = when (state) {
        DevToolsViewModel.ActionState.Idle      -> "" to InkSecondary
        DevToolsViewModel.ActionState.Running   -> "Working…" to InkSecondary
        is DevToolsViewModel.ActionState.Success -> "✓ ${state.message}" to AccentGreen
        is DevToolsViewModel.ActionState.Error   -> "✗ ${state.message}" to DeleteRed
    }
    if (text.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
