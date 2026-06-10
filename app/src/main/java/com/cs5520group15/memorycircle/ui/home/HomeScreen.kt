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
import com.cs5520group15.memorycircle.ui.common.MemoryCircleBottomNav
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Main home screen showing a greeting, recent groups list, and bottom navigation.
 * Who: Called by MemoryCircleNavigation after successful login.
 * When: Displayed when the user is on the Home tab.
 */
@Composable
fun HomeScreen(
    currentRoute:       String,
    onNavigate:         (Any) -> Unit,
    onCreateScrapbook:  (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val groups   by viewModel.groups.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()

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
                onClick            = { onCreateScrapbook("new") },
                containerColor     = Ink,
                contentColor       = Cream
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

            // Greeting row
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.Top
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
                AvatarCircle(name = userName, size = 44.dp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Section header
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = "RECENT GROUPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink
                )
                TextButton(onClick = {}) {
                    Text(
                        text  = "See all",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Brown
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Groups list
            // key = { it.id } so Compose tracks each card by its unique ID
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups, key = { it.id }) { group ->
                    GroupCard(
                        group    = group,
                        onClick  = { onCreateScrapbook(group.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MemoryCircleTheme {
        HomeScreen(
            currentRoute      = "home",
            onNavigate        = {},
            onCreateScrapbook = {}
        )
    }
}