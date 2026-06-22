/**
 * What: Jetpack Compose UI for the Dev Tools screen — developer utilities such as
 *       seeding test data.
 * Who:  Wired into the nav graph by MemoryCircleNavigation; reached from SettingsScreen
 *       via its "open dev tools" action.
 * When: Composed when the user navigates to the DevTools route from Settings.
 */

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.common.PrimaryButton
import com.cs5520group15.memorycircle.ui.theme.*
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

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
    val acceptForU6State   by viewModel.acceptForU6State.collectAsStateWithLifecycle()
    val simFriendReqState   by viewModel.simFriendReqState.collectAsStateWithLifecycle()
    val simGroupInviteState by viewModel.simGroupInviteState.collectAsStateWithLifecycle()
    val simJoinMyGroupState by viewModel.simJoinMyGroupState.collectAsStateWithLifecycle()
    val simNewPostState     by viewModel.simNewPostState.collectAsStateWithLifecycle()
    val simNewPhotoState    by viewModel.simNewPhotoState.collectAsStateWithLifecycle()
    val simCommentState     by viewModel.simCommentState.collectAsStateWithLifecycle()

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
                description = "Patches a themed bio + a stable Firebase Storage avatar URL onto every test user (1-10@test.com) so member lists, group avatar collages, and friend rows don't all show the same Sage letter. Also upgrades any old-shape doc to the new public shape (emailMasked).",
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

            SeedSection(
                title       = "Accept friend requests for Test User 6",
                description = "Impersonates 6@test.com and accepts every friend request currently sitting in their inbox. Use after sending Test User 6 an invitation from AddFriendSearch — Firestore Console first confirms the request landed, then this button promotes it to a real friendship without signing in as 6@test.com.",
                buttonLabel = "Accept for Test User 6",
                state       = acceptForU6State,
                onRun       = { viewModel.acceptForU6() }
            )

            Text(
                text  = "NOTIFICATION SIMULATIONS",
                style = MaterialTheme.typography.labelSmall,
                color = Ink
            )

            SeedSection(
                title       = "1. Friend request from Test User 8",
                description = "Writes an incoming-request doc on your inbox as if 8@test.com tapped Add on you. Triggers the friend-request notification on this device.",
                buttonLabel = "Simulate friend request",
                state       = simFriendReqState,
                onRun       = { viewModel.simFriendRequestFromU8() }
            )

            SeedSection(
                title       = "2. Test User 8 creates a group and adds you",
                description = "Creates a sim group owned by 8@test.com with you as a member (idempotent — reuses the same sim group on re-tap). Triggers the group-invite notification, and seeds the group that the next three sims write into.",
                buttonLabel = "Simulate group invite",
                state       = simGroupInviteState,
                onRun       = { viewModel.simGroupInviteFromU8() }
            )

            SeedSection(
                title       = "3. Test User 10 joins your owned group",
                description = "Adds 10@test.com to the memberIds of YOUR first owned group. Triggers the new-member notification (only fires for the owner — you).",
                buttonLabel = "Simulate Test User 10 joining",
                state       = simJoinMyGroupState,
                onRun       = { viewModel.simU10JoinMyGroup() }
            )

            SeedSection(
                title       = "4. Test User 8 posts in their sim group",
                description = "Inserts a new post authored by 8@test.com into the current-month scrapbook of the sim group from step 2 (auto-creates the group if you skipped step 2). Triggers the new-post notification.",
                buttonLabel = "Simulate new post",
                state       = simNewPostState,
                onRun       = { viewModel.simNewPostByU8() }
            )

            SeedSection(
                title       = "5. Test User 8 adds a photo to their post",
                description = "Appends a new photo to the latest post authored by 8@test.com in the sim group (auto-creates a post if none exists). Triggers the new-photo notification.",
                buttonLabel = "Simulate new photo",
                state       = simNewPhotoState,
                onRun       = { viewModel.simNewPhotoByU8() }
            )

            SeedSection(
                title       = "6. Test User 8 comments on their post",
                description = "Adds a comment authored by 8@test.com on the latest post by 8@test.com in the sim group (auto-creates a post if none exists). Triggers the new-comment notification.",
                buttonLabel = "Simulate new comment",
                state       = simCommentState,
                onRun       = { viewModel.simCommentByU8() }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun DevToolsScreenPreview() {
    MemoryCircleTheme {
        DevToolsScreen(onBack = {})
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

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun SeedSectionPreview() {
    MemoryCircleTheme {
        SeedSection(
            title       = "Seed Users",
            description = "Create sample user accounts",
            buttonLabel = "Run",
            state       = DevToolsViewModel.ActionState.Idle,
            onRun       = {}
        )
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

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun ResultLinePreview() {
    MemoryCircleTheme {
        ResultLine(state = DevToolsViewModel.ActionState.Success("Seeded 10 users"))
    }
}
