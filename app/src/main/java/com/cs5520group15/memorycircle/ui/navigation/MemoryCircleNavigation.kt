package com.cs5520group15.memorycircle.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cs5520group15.memorycircle.ui.auth.LoginScreen
import com.cs5520group15.memorycircle.ui.auth.RegisterScreen
import com.cs5520group15.memorycircle.ui.group.CreateGroupScreen
import com.cs5520group15.memorycircle.ui.group.GroupMembersScreen
import com.cs5520group15.memorycircle.ui.home.HomeScreen
import com.cs5520group15.memorycircle.ui.scrapbook.ScrapbookScreen
import com.cs5520group15.memorycircle.ui.scrapbook.ScrapbookViewerScreen
import com.cs5520group15.memorycircle.ui.memories.MemoriesScreen

/**
 * What: Sets up the entire navigation graph for the app.
 *       Connects all screens to their routes and handles navigation events.
 * Who: Called by MainActivity to launch the app's navigation system.
 * When: Executed once when the app starts.
 */
@Composable
fun MemoryCircleNavigation() {

    // rememberNavController creates and remembers the navigation controller
    // It manages the back stack (which screens the user has visited)
    val navController = rememberNavController()

    // currentBackStackEntryAsState lets us know which screen is currently active
    // We use this to highlight the correct tab in the bottom nav bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    // The bottom nav bar emits plain String routes ("home", "memories", ...).
    // This app uses type-safe routes (Serializable objects), so we must map the
    // String to its destination object before navigating. Passing the raw String
    // to navController.navigate() does NOT match any destination and crashes.
    // Unregistered tabs (friends/profile) map to null and are ignored for now.
    val onTabSelected: (Any) -> Unit = { route ->
        val destination: Any? = when (route) {
            "home"     -> Home
            "memories" -> Memories
            else       -> null
        }
        if (destination != null) {
            navController.navigate(destination) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState    = true
            }
        }
    }

    NavHost(
        navController    = navController,
        startDestination = Login
    ) {

        // Login screen
        // onLoginSuccess → navigate to Home and clear the back stack
        // (so pressing back from Home doesn't go back to Login)
        composable<Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Home) {
                        popUpTo(Login) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Register)
                }
            )
        }

        // Register screen
        // onRegisterSuccess → navigate to Home and clear Login + Register from back stack
        composable<Register> {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Home) {
                        popUpTo(Login) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Home screen
        // Passes currentRoute so BottomNav knows which tab to highlight
        composable<Home> {
            HomeScreen(
                currentRoute  = currentRoute,
                onNavigate    = onTabSelected,
                onCreateGroup = {
                    // Home "+" → pick contacts to create a new group
                    navController.navigate(CreateGroup)
                },
                onOpenGroup   = { groupId ->
                    // Tap a group card → open that group's collaborative timeline
                    navController.navigate(ScrapbookViewer(groupId))
                }
            )
        }

        // Scrapbook creation screen — create a new time point or join an existing one
        // onSaved → pop back to the timeline, which re-collects the updated entries
        composable<ScrapbookDetail> { entry ->
            val detail = entry.toRoute<ScrapbookDetail>()
            ScrapbookScreen(
                groupId = detail.groupId,
                entryId = detail.entryId,
                onBack  = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // Scrapbook viewer — a group's collaborative timeline of memory time points
        composable<ScrapbookViewer> { entry ->
            val detail = entry.toRoute<ScrapbookViewer>()
            ScrapbookViewerScreen(
                groupId        = detail.groupId,
                onBack         = { navController.popBackStack() },
                onOpenMembers  = { navController.navigate(GroupMembers(detail.groupId)) },
                onAddTimePoint = { navController.navigate(ScrapbookDetail(detail.groupId)) },
                onJoinEntry    = { entryId ->
                    navController.navigate(ScrapbookDetail(detail.groupId, entryId))
                }
            )
        }

        composable<Memories> {
            MemoriesScreen(
                currentRoute    = currentRoute,
                onNavigate      = onTabSelected,
                onOpenScrapbook = { groupId ->
                    // Tap an existing scrapbook → view its timeline (read-only tab)
                    navController.navigate(ScrapbookViewer(groupId))
                }
            )
        }

        // Create-a-new-group screen (contact picker) — placeholder for now
        composable<CreateGroup> {
            CreateGroupScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // A group's members page — placeholder for now (owned by a teammate)
        composable<GroupMembers> { entry ->
            val detail = entry.toRoute<GroupMembers>()
            GroupMembersScreen(
                groupId = detail.groupId,
                onBack  = { navController.popBackStack() }
            )
        }
    }
}