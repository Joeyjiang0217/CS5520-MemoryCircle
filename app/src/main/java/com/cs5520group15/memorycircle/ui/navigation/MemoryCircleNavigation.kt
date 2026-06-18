package com.cs5520group15.memorycircle.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cs5520group15.memorycircle.data.AuthRepository
import com.cs5520group15.memorycircle.ui.addfriend.AddFriendScreen
import com.cs5520group15.memorycircle.ui.addfriendsearch.AddFriendSearchScreen
import com.cs5520group15.memorycircle.ui.avatarviewer.AvatarViewerScreen
import com.cs5520group15.memorycircle.ui.creategroup.CreateGroupScreen
import com.cs5520group15.memorycircle.ui.devtools.DevToolsScreen
import com.cs5520group15.memorycircle.ui.editprofile.EditProfileScreen
import com.cs5520group15.memorycircle.ui.friendrequests.AllFriendRequestsScreen
import com.cs5520group15.memorycircle.ui.friends.FriendsScreen
import com.cs5520group15.memorycircle.ui.friendsearch.FriendsSearchScreen
import com.cs5520group15.memorycircle.ui.groupdetail.GroupDetailScreen
import com.cs5520group15.memorycircle.ui.groupmembers.GroupMembersScreen
import com.cs5520group15.memorycircle.ui.home.HomeScreen
import com.cs5520group15.memorycircle.ui.login.LoginScreen
import com.cs5520group15.memorycircle.ui.memberprofile.MemberProfileScreen
import com.cs5520group15.memorycircle.ui.memories.MemoriesScreen
import com.cs5520group15.memorycircle.ui.notifications.NotificationSettingsScreen
import com.cs5520group15.memorycircle.ui.profile.ProfileScreen
import com.cs5520group15.memorycircle.ui.register.RegisterScreen
import com.cs5520group15.memorycircle.ui.scrapbook.ScrapbookScreen
import com.cs5520group15.memorycircle.ui.scrapbookhistory.ScrapbookHistoryScreen
import com.cs5520group15.memorycircle.ui.scrapbookviewer.ScrapbookViewerScreen
import com.cs5520group15.memorycircle.ui.settings.SettingsScreen

/**
 * What: Sets up the entire navigation graph for the app.
 *       Connects all screens to their routes and handles navigation events.
 * Who: Called by MainActivity to launch the app's navigation system.
 * When: Executed once when the app starts.
 */
@Composable
fun MemoryCircleNavigation() {

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    // The bottom nav bar emits plain String routes ("home", "memories", ...).
    // This app uses type-safe routes (Serializable objects), so we must map the
    // String to its destination object before navigating. Passing the raw String
    // to navController.navigate() does NOT match any destination and crashes.
    val onTabSelected: (Any) -> Unit = { route ->
        val destination: Any? = when (route) {
            "home"     -> Home
            "memories" -> Memories
            "friends"  -> Friends
            "profile"  -> Profile
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

    // Auth persistence: if Firebase already has a signed-in user (cached on
    // disk by the SDK across app restarts), skip Login and land on Home.
    val startDestination: Any =
        if (AuthRepository.currentUid != null) Home else Login

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {

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

        composable<Home> {
            HomeScreen(
                currentRoute  = currentRoute,
                onNavigate    = onTabSelected,
                onCreateGroup = {
                    navController.navigate(CreateGroup())
                },
                onOpenGroup   = { groupId ->
                    navController.navigate(ScrapbookViewer(groupId))
                }
            )
        }

        composable<ScrapbookDetail> { entry ->
            val detail = entry.toRoute<ScrapbookDetail>()
            ScrapbookScreen(
                groupId = detail.groupId,
                entryId = detail.entryId,
                onBack  = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable<ScrapbookHistory> { entry ->
            val detail = entry.toRoute<ScrapbookHistory>()
            ScrapbookHistoryScreen(
                groupId = detail.groupId,
                month   = detail.month,
                year    = detail.year,
                onBack  = { navController.popBackStack() }
            )
        }

        composable<ScrapbookViewer> { entry ->
            val detail = entry.toRoute<ScrapbookViewer>()
            ScrapbookViewerScreen(
                groupId           = detail.groupId,
                onBack            = { navController.popBackStack() },
                onOpenGroupDetail = { navController.navigate(GroupDetail(detail.groupId)) },
                onAddTimePoint    = { navController.navigate(ScrapbookDetail(detail.groupId)) },
                onJoinEntry       = { entryId ->
                    navController.navigate(ScrapbookDetail(detail.groupId, entryId))
                }
            )
        }

        composable<Memories> {
            MemoriesScreen(
                currentRoute    = currentRoute,
                onNavigate      = onTabSelected,
                onOpenScrapbook = { groupId, month, year ->
                    navController.navigate(ScrapbookHistory(groupId, month, year))
                }
            )
        }

        composable<CreateGroup> { entry ->
            val args = entry.toRoute<CreateGroup>()
            CreateGroupScreen(
                onBack       = { navController.popBackStack() },
                isInviteMode = args.isInviteMode,
                onCreated    = { newGroupId ->
                    // Pop the picker off the back stack before landing on the
                    // new group's timeline so the system Back button returns
                    // to Home, not back into the contact picker.
                    navController.navigate(ScrapbookViewer(newGroupId)) {
                        popUpTo<CreateGroup> { inclusive = true }
                    }
                },
                onInvite     = { _ ->
                    // Invite-confirm: actual member-add wiring against the
                    // parent group's repository lands in a follow-up turn.
                    // For now just unwind to GroupDetail.
                    navController.popBackStack()
                }
            )
        }

        composable<GroupMembers> { entry ->
            val detail = entry.toRoute<GroupMembers>()
            GroupMembersScreen(
                groupId             = detail.groupId,
                onBack              = { navController.popBackStack() },
                onOpenMemberProfile = { userId -> navController.navigate(MemberProfile(userId)) }
            )
        }

        composable<Friends> {
            FriendsScreen(
                currentRoute        = currentRoute,
                onNavigate          = onTabSelected,
                onOpenSearch        = { navController.navigate(FriendsSearch) },
                onOpenAllRequests   = { navController.navigate(AllFriendRequests) },
                onOpenAddFriend     = { navController.navigate(AddFriend) },
                onOpenMemberProfile = { userId -> navController.navigate(MemberProfile(userId)) },
                onOpenGroupDetail   = { groupId -> navController.navigate(GroupDetail(groupId)) }
            )
        }

        composable<AddFriend> {
            AddFriendScreen(
                onBack       = { navController.popBackStack() },
                onOpenSearch = { navController.navigate(AddFriendSearch) }
            )
        }

        composable<AddFriendSearch> {
            AddFriendSearchScreen(
                onCancel            = { navController.popBackStack() },
                onOpenMemberProfile = { userId -> navController.navigate(MemberProfile(userId)) }
            )
        }

        composable<Profile> {
            ProfileScreen(
                currentRoute      = currentRoute,
                onNavigate        = onTabSelected,
                onOpenEditProfile = { navController.navigate(EditProfile) },
                onOpenSettings    = { navController.navigate(Settings) }
            )
        }

        composable<EditProfile> {
            EditProfileScreen(
                onBack             = { navController.popBackStack() },
                onOpenAvatarViewer = { navController.navigate(AvatarViewer) }
            )
        }

        composable<AvatarViewer> {
            AvatarViewerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<Settings> {
            SettingsScreen(
                onBack                      = { navController.popBackStack() },
                onOpenProfile               = { navController.navigate(EditProfile) },
                onOpenNotificationSettings  = { navController.navigate(NotificationSettings) },
                onOpenDevTools              = { navController.navigate(DevTools) },
                onLogout                    = {
                    AuthRepository.logout()
                    navController.navigate(Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<NotificationSettings> {
            NotificationSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<DevTools> {
            DevToolsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<MemberProfile> { entry ->
            val detail = entry.toRoute<MemberProfile>()
            MemberProfileScreen(
                userId = detail.userId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<AllFriendRequests> {
            AllFriendRequestsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<FriendsSearch> {
            FriendsSearchScreen(
                onCancel             = { navController.popBackStack() },
                onOpenMemberProfile  = { userId -> navController.navigate(MemberProfile(userId)) },
                onOpenGroupDetail    = { groupId ->
                    navController.navigate(GroupDetail(groupId))
                }
            )
        }

        composable<GroupDetail> { entry ->
            val detail = entry.toRoute<GroupDetail>()
            GroupDetailScreen(
                groupId             = detail.groupId,
                onBack              = { navController.popBackStack() },
                onOpenAllMembers    = { navController.navigate(GroupMembers(detail.groupId)) },
                onOpenMemberProfile = { userId -> navController.navigate(MemberProfile(userId)) },
                onInviteMember      = { navController.navigate(CreateGroup(isInviteMode = true)) },
                onOpenScrapbook     = { gid, month, year ->
                    navController.navigate(ScrapbookHistory(gid, month, year))
                },
                onLeftGroup         = {
                    // After leaving a group, popBackStack would land on this
                    // group's ScrapbookViewer — exactly what we just left.
                    // Navigate to Home and clear everything above it instead.
                    navController.navigate(Home) {
                        popUpTo(Home) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
