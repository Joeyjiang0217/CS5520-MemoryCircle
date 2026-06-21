/**
 * What: Jetpack Compose UI for the Home screen — the post-login landing surface
 *       showing the user's groups with create/open actions.
 * Who:  Wired into the nav graph by MemoryCircleNavigation; reached as the start
 *       destination after a successful login/register or when a session is restored.
 * When: Composed when the user navigates to the Home route (the start destination
 *       when a signed-in user is present).
 */

package com.cs5520group15.memorycircle.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.GroupCard
import com.cs5520group15.memorycircle.ui.common.MemoryCircleBottomNav
import com.cs5520group15.memorycircle.ui.common.SectionHeader
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Main home screen showing a greeting, recent groups list, and bottom navigation.
 * Who: Called by MemoryCircleNavigation after successful login.
 * When: Displayed when the user is on the Home tab.
 */
@Composable
fun HomeScreen(
    currentRoute:   String,
    onNavigate:     (Any) -> Unit,
    onCreateGroup:  () -> Unit,
    onOpenGroup:    (String) -> Unit,
    viewModel:      HomeViewModel = viewModel()
) {
    val groups   by viewModel.groups.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val profile  by viewModel.profile.collectAsStateWithLifecycle()

    HomeContent(
        groups        = groups,
        userName      = userName,
        avatarUrl     = profile.avatarUrl,
        currentRoute  = currentRoute,
        onNavigate    = onNavigate,
        onCreateGroup = onCreateGroup,
        onOpenGroup   = onOpenGroup
    )
}

/**
 * What: Stateless content of the Home screen — renders the greeting, recent
 *       groups list, and bottom navigation purely from its parameters, so it
 *       can be shown in a @Preview without constructing a (Firebase-backed) ViewModel.
 * Who:  Used by HomeScreen, which supplies the live ViewModel-backed state.
 * When: Composed by HomeScreen on every recomposition.
 */
@Composable
private fun HomeContent(
    groups:        List<HomeViewModel.Group>,
    userName:      String,
    avatarUrl:     String,
    currentRoute:  String,
    onNavigate:    (Any) -> Unit,
    onCreateGroup: () -> Unit,
    onOpenGroup:   (String) -> Unit
) {
    Scaffold(
        containerColor = Cream,
        bottomBar = {
            MemoryCircleBottomNav(
                currentRoute = currentRoute,
                onNavigate   = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { onCreateGroup() },
                containerColor = Ink,
                contentColor   = Cream
            ) {
                Icon(
                    painter            = painterResource(R.drawable.ic_add),
                    contentDescription = "Create new group"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column {
                    Text(
                        text  = "Good morning,",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text  = userName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Brown
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✦", color = Ink)
                    }
                }
                AvatarCircle(name = userName, size = 44.dp, photoUrl = avatarUrl)
            }

            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader(
                text = "RECENT GROUPS",
                trailing = {
                    TextButton(onClick = {}) {
                        Text(
                            text  = "See all",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Brown
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups, key = { it.id }) { group ->
                    GroupCard(
                        name        = group.name,
                        date        = group.date,
                        memoryCount = group.memoryCount,
                        colorType   = group.colorType,
                        onClick     = { onOpenGroup(group.id) },
                        modifier    = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — each one targets the whole screen at a different UI state.
// ---------------------------------------------------------------------------

private val previewGroups = listOf(
    HomeViewModel.Group(
        id          = "g1",
        name        = "Summer Trip",
        date        = "5 members",
        memoryCount = 12,
        colorType   = "brown"
    ),
    HomeViewModel.Group(
        id          = "g2",
        name        = "Family",
        date        = "3 members",
        memoryCount = 7,
        colorType   = "sage"
    ),
    HomeViewModel.Group(
        id          = "g3",
        name        = "Weekend Hike",
        date        = "4 members",
        memoryCount = 3,
        colorType   = "brown"
    )
)

/** Default — signed-in user with a few groups. */
@Preview(showBackground = true, name = "Home · default")
@Composable
fun HomeScreenPreview() {
    MemoryCircleTheme {
        HomeContent(
            groups        = previewGroups,
            userName      = "Ada",
            avatarUrl     = "",
            currentRoute  = "home",
            onNavigate    = {},
            onCreateGroup = {},
            onOpenGroup   = {}
        )
    }
}

/** Empty groups — new user has not joined / created any group yet. */
@Preview(showBackground = true, name = "Home · empty")
@Composable
fun HomeScreenEmptyPreview() {
    MemoryCircleTheme {
        HomeContent(
            groups        = emptyList(),
            userName      = "Ada",
            avatarUrl     = "",
            currentRoute  = "home",
            onNavigate    = {},
            onCreateGroup = {},
            onOpenGroup   = {}
        )
    }
}
