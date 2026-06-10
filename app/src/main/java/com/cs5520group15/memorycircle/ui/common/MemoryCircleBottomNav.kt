package com.cs5520group15.memorycircle.ui.common

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme
import com.cs5520group15.memorycircle.ui.theme.Brown
import com.cs5520group15.memorycircle.ui.theme.Beige

/**
 * What: Data class representing a single bottom navigation tab.
 * Who: Used by MemoryCircleBottomNav to render each tab item.
 * When: Instantiated when defining the list of nav items.
 */
data class BottomNavItem(
    val label:    String,
    val iconRes:  Int,
    val route:    String
)

/**
 * What: Reusable bottom navigation bar for the main app screens.
 * Who: Called by HomeScreen and other main screens inside a Scaffold.
 * When: Visible on all main screens after login.
 */
@Composable
fun MemoryCircleBottomNav(
    currentRoute: String,
    onNavigate:   (String) -> Unit
) {
    val items = listOf(
        BottomNavItem("Home",     R.drawable.ic_home,    "home"),
        BottomNavItem("Memories", R.drawable.ic_image,   "memories"),
        BottomNavItem("Friends",  R.drawable.ic_friends, "friends"),
        BottomNavItem("Profile",  R.drawable.ic_profile, "profile")
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = androidx.compose.ui.unit.Dp(0f)
    ) {
        items.forEach { item ->
            NavigationBarItem(
                // Use contains() because currentRoute is a full class path
                // e.g. "com.cs5520group15.memorycircle.ui.navigation.Home"
                // so we just check if it contains "home", "memories", etc.
                selected = currentRoute.contains(item.route, ignoreCase = true),
                onClick  = { onNavigate(item.route) },
                icon = {
                    Icon(
                        painter            = painterResource(id = item.iconRes),
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text  = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Brown,
                    selectedTextColor   = Brown,
                    unselectedIconColor = Beige,
                    unselectedTextColor = Beige,
                    indicatorColor      = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MemoryCircleBottomNavPreview() {
    MemoryCircleTheme {
        MemoryCircleBottomNav(
            currentRoute = "home",
            onNavigate   = {}
        )
    }
}