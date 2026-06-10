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
import com.cs5520group15.memorycircle.ui.home.HomeScreen
import com.cs5520group15.memorycircle.ui.scrapbook.ScrapbookScreen
import com.cs5520group15.memorycircle.ui.scrapbook.ScrapbookViewerScreen

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
                currentRoute = currentRoute,
                onNavigate   = { route ->
                    navController.navigate(route) {
                        // Keeps the back stack clean when switching tabs
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                onCreateScrapbook = { groupId ->
                    navController.navigate(ScrapbookDetail(groupId))
                }
            )
        }

        // Scrapbook creation screen
        // onGenerate → navigate to ScrapbookViewer with the same groupId
        composable<ScrapbookDetail> { entry ->
            val detail = entry.toRoute<ScrapbookDetail>()
            ScrapbookScreen(
                groupId    = detail.groupId,
                onBack     = { navController.popBackStack() },
                onGenerate = {
                    navController.navigate(ScrapbookViewer(detail.groupId))
                }
            )
        }

        // Scrapbook viewer screen
        // Displays generated pages (HorizontalPager) + timeline
        composable<ScrapbookViewer> { entry ->
            val detail = entry.toRoute<ScrapbookViewer>()
            ScrapbookViewerScreen(
                groupId = detail.groupId,
                onBack  = { navController.popBackStack() }
            )
        }
    }
}