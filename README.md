# MemoryCircle

A collaborative Android app for small friend / family groups to build a shared
monthly "scrapbook" of photos. Members of a group contribute time points
(photo + caption) to the current month's scrapbook, browse past months, and
manage their friend graph and group memberships from a single navigation hub.

---

## Demo Walkthrough

### Demo account

For this walkthrough we'll use **`a@test.com`** with password **`123456`**.
This is an **admin account** — a few of the actions on the **Dev Tools**
screen are gated to admin users (you can verify the restriction by registering
a fresh regular account, but for the full feature tour we strongly recommend
signing in with the admin account).

### 1. Launch and login

When you launch the app, you'll see our **custom splash screen** — the
Memory Circle logo with a short text animation — before the app routes you to
the **Login** screen.

Enter `a@test.com / 123456` and tap **Log In**. You'll land on the **Home**
screen. The admin account starts out with **no groups**, but it already has
one default friend — **Test User 1** — pre-added so we can immediately create
a group with someone.

### 2. Create your first group

Tap the **create group** action on Home, give the group a name, and add
**Test User 1** as a member. The group is created and now shows up on the
Home screen.

### 3. Seed memories from Dev Tools

Open **Profile → Settings → Dev Tools**.

- Tap **Seed test post** — a freshly seeded post is inserted into the current
  month's scrapbook of the group you just created. Pop back into that group
  and you'll see your own seeded post on the timeline.
- Tap **Seed history** — the past three months of scrapbooks are created for
  the same group, each pre-populated with two seeded posts. Combined with
  this month, that's **four scrapbooks total**, and all four also appear on
  the **Memories** tab as monthly cover thumbnails.

### 4. Add a friend via search

Open the **Friends** screen and use the **Add friend** search. Type "**test**"
— every test user shows up in the results. Find **Test User 6** and tap
**Add**; an invitation is sent.

Back in **Dev Tools**, tap **Accept for Test User 6**. This impersonates Test
User 6 and accepts the request you just sent, so the friendship becomes real.
Test User 6 now appears in your **Friends** list. The Friends screen also
shows **every group you belong to**, so the group you created in step 2 is
listed here as well.

### 5. Seed and clear test-user profiles

Still in **Dev Tools**:

- Tap **Seed profiles** — every test user (1–10) gets a themed bio plus a
  stock photo avatar served from Firebase Storage. Go back to the **Friends**
  screen, and you'll see the default Sage-letter avatars replaced with real
  photos and the bios filled in.
- Tap **Clear profiles** — the bio and avatar are blanked, and the Friends
  screen falls back to the default letter avatars. (Running clear → seed is a
  nice way to demo the cross-device refresh.)

### 6. Notification simulations

The remaining Dev Tools buttons simulate real-world activity. **Notifications
are on by default**, so each action also pushes a system notification on the
device.

- **Simulate friend request** — Test User 8 sends you a friend request. You
  receive a **friend-request notification**. Open the **Friends** screen,
  find the incoming-request row, and tap **Accept** — Test User 8 is now in
  your friends list.
- **Simulate group invite** — Test User 8 creates a new group and adds you
  to it. You receive a **group-invite notification**, and back on the
  **Home** screen the new group appears.
- **Simulate new post**, **Simulate new photo**, **Simulate new comment** —
  Test User 8 publishes a post in that sim group's current-month scrapbook,
  then appends a second photo to that same post, then leaves a comment on
  it. You receive **three separate notifications**, and the sim group's
  scrapbook ends up with **one post containing two photos and one comment**.
- **Simulate Test User 10 joining** — Test User 10 joins the group you
  created back in step 2. Because you're the owner of that group, you
  receive a **new-member notification**.

---

## Screen-by-Screen Reference

The rest of this README is a screen-by-screen walkthrough of the app. Every
screen is shown with the screenshot from `ui screens/`, in the order a user
actually encounters them.

---

## 1. Auth

### LoginScreen
The app launches on **LoginScreen**. Existing users sign in with email and
password. Tapping **Create Account** opens RegisterScreen.

<img src="ui%20screens/loginScreen.png" width="280" alt="Login screen" />

### RegisterScreen
**RegisterScreen** is the new-account form (name, email, password). On
successful registration the back stack is cleared and the user lands directly
on Home (so the system Back button does not return to the form).

<img src="ui%20screens/registerScreen.png" width="280" alt="Register screen" />

---

## 2. Home tab

### HomeScreen
After login the user lands on **HomeScreen** — a list of every group they
belong to, plus a "+" FAB for creating a new group.

<img src="ui%20screens/homeScreen.png" width="280" alt="Home screen" />

### ScrapbookViewerScreen
Tapping **Group 1** opens **ScrapbookViewerScreen**, that group's live
collaborative timeline for the current month. Each row is one time point — a
photo + caption contributed by one member.

<img src="ui%20screens/scrapbookViewerScreen.png" width="280" alt="Scrapbook viewer screen" />

### ScrapbookScreen
From the viewer you can contribute in two ways, both of which open
**ScrapbookScreen** (the single time-point editor):
- Tapping **Add photo** on an existing entry → join that time point with your
  own photo + description.
- Tapping the **"+"** floating button → create a brand-new time point for the
  current month.

<img src="ui%20screens/scrapbookScreen.png" width="280" alt="Scrapbook editor screen" />

### GroupDetailScreen
Tapping the **三 (hamburger)** icon at the top-right of the timeline opens
**GroupDetailScreen** — the group's settings + history hub. The top-right
icon here and the red **Leave group** button at the bottom both let the user
leave the group.

<img src="ui%20screens/groupDetailScreen.png" width="280" alt="Group detail screen" />

### GroupMembersScreen
Tapping **View all** on the members section opens **GroupMembersScreen**, the
full alphabetical roster for this group.

<img src="ui%20screens/groupMembersScreen.png" width="280" alt="Group members screen" />

### MemberProfileScreen (from GroupMembers)
Tapping any user in the roster opens **MemberProfileScreen** — the read-only
profile view for any user other than the current one.

<img src="ui%20screens/memberProfileScreen.png" width="280" alt="Member profile screen" />

### MemberProfileScreen (from GroupDetail thumbnails)
Going back to GroupDetailScreen, tapping any of the **member thumbnail
avatars** in the grid also opens that user's MemberProfileScreen — the same
destination, reached from a different surface.

### InviteNewMemberScreen
Back on GroupDetailScreen, tapping **Invite** opens
**InviteNewMemberScreen**. (Under the hood this is the CreateGroup contact
picker re-used in invite mode — the top-bar title and CTA label flip, and the
confirm action wires the new members back into the existing group instead of
minting a new one.)

<img src="ui%20screens/inviteNewMemberScreen.png" width="280" alt="Invite new member screen" />

### ScrapbookHistryScreen
Back on GroupDetailScreen, the per-month scrapbook list at the bottom shows
every past month for this group. Tapping a past month opens
**ScrapbookHistryScreen**.

Compared to ScrapbookViewerScreen, the history screen is missing **Add
photo**, the **"+"** floating button, and the top-right **三** icon — because
these are historical records, only viewing is supported.

<img src="ui%20screens/scrapbookHistryScreen.png" width="280" alt="Scrapbook history screen" />

### NewGroupScreen
Returning to HomeScreen, tapping the **"+"** FAB opens **NewGroupScreen** —
the contact picker for choosing initial members of a brand-new group.

<img src="ui%20screens/newGroupScreen.png" width="280" alt="New group screen" />

### EmptyScrapbookHistoryScreen
Tapping **Create now** mints the group and drops the user into the new
group's ScrapbookViewer. Because the new group has zero memory records yet,
it renders the empty / initial state — **EmptyScrapbookHistoryScreen**.

<img src="ui%20screens/emptyScrapbookHistoryScreen.png" width="280" alt="Empty scrapbook history screen" />

---

## 3. Memories tab

### MemoriesScreen
**MemoriesScreen** is a read-only calendar of past months across all of the
user's groups.

<img src="ui%20screens/memoriesScreen.png" width="280" alt="Memories screen" />

Tapping a month entry opens **ScrapbookHistryScreen** for that group / month /
year — the same read-only view reached from GroupDetail.

<img src="ui%20screens/scrapbookHistryScreen.png" width="280" alt="Scrapbook history screen" />

---

## 4. Friends tab

### FriendsScreen
**FriendsScreen** is the friend graph hub.

<img src="ui%20screens/FriendsScreen.png" width="280" alt="Friends screen" />

### AddFriendScreen
Tapping the **add-friend icon** at the top-right opens **AddFriendScreen** —
the landing page for the "add a new friend" flow. It holds only a tap-only
search bar.

<img src="ui%20screens/addFriendScreen.png" width="280" alt="Add friend screen" />

### AddFriendSearchScreen
Tapping the search bar on AddFriendScreen opens **AddFriendSearchScreen**,
the active search overlay with an auto-focused text field. Results are people
only.

<img src="ui%20screens/addFriendSearchScreen.png" width="280" alt="Add friend search screen" />

### FriendsSearchScreen
Back on FriendsScreen, tapping the hero **search bar** opens
**FriendsSearchScreen** — a full-screen overlay that fuzzy-matches both
friends (by name or email) and groups (by name).

<img src="ui%20screens/friendsSearchScreen.png" width="280" alt="Friends search screen" />

### AllFriendRequestsScreen
Back on FriendsScreen, tapping **See all** in the FRIEND REQUESTS section
opens **AllFriendRequestsScreen**, which shows every request
(pending / accepted / declined) and supports per-row swipe-to-dismiss.

<img src="ui%20screens/allFriendRequestsScreen.png" width="280" alt="All friend requests screen" />

Tapping any user row in this list opens that user's **MemberProfileScreen**.

### MemberProfileScreen (from FriendsScreen)
Back on FriendsScreen, tapping any user row in the friends list also opens
that user's **MemberProfileScreen** — the same read-only profile destination.

<img src="ui%20screens/memberProfileScreen.png" width="280" alt="Member profile screen" />

---

## 5. Profile tab

### ProfileScreen
**ProfileScreen** is the current user's own profile card.

<img src="ui%20screens/profileScreen.png" width="280" alt="Profile screen" />

### EditProfileScreen
Tapping **Edit Profile** opens **EditProfileScreen** — a form with rows for
avatar, name, bio, and email; each row is tap-to-edit.

<img src="ui%20screens/editProfileScreen.png" width="280" alt="Edit profile screen" />

### SettingScreen
Back on ProfileScreen, tapping **Settings** opens **SettingScreen** — the
account settings hub.

<img src="ui%20screens/settingScreen.png" width="280" alt="Settings screen" />

### EditProfileScreen (from Settings)
Tapping the **Profile** row on SettingScreen opens **EditProfileScreen**
again — the same destination reached from ProfileScreen.

### NotificationSettingsScreen
Tapping the **Notifications** row on SettingScreen opens
**NotificationSettingsScreen**, which holds the per-channel notification
toggles.

<img src="ui%20screens/notificationSettingsScreen.png" width="280" alt="Notification settings screen" />

---

## 6. Navigation map

The whole UI interaction graph, rooted at the single NavHost:

```text
[MemoryCircleNavigation]  (Owns the NavHost + bottom-nav state)
       │
       ▼
   [NavHost]  startDestination = Login
       │
       ├─► [LoginScreen]
       │      ├─ onLoginSuccess() ─────────► [HomeScreen]   (clears stack up to Login)
       │      └─ onNavigateToRegister() ───► [RegisterScreen]
       │
       ├─► [RegisterScreen]
       │      ├─ onRegisterSuccess() ──────► [HomeScreen]   (clears stack up to Login)
       │      └─ onNavigateToLogin() ──────► back
       │
       ├─► [HomeScreen]                                    ◄── bottom-nav tab
       │      ├─ onCreateGroup() ──────────► [NewGroupScreen]        (CreateGroup, default mode)
       │      └─ onOpenGroup(groupId) ─────► [ScrapbookViewerScreen]
       │
       ├─► [ScrapbookViewerScreen]
       │      ├─ onOpenGroupDetail() ──────► [GroupDetailScreen]
       │      ├─ onAddTimePoint() ─────────► [ScrapbookScreen]       (new entry)
       │      └─ onJoinEntry(entryId) ─────► [ScrapbookScreen]       (join existing entry)
       │
       ├─► [ScrapbookScreen]
       │      ├─ onBack() ─────────────────► back
       │      └─ onSaved() ────────────────► back
       │
       ├─► [ScrapbookHistryScreen]    (read-only: no add / no FAB / no menu)
       │      └─ onBack() ─────────────────► back
       │
       ├─► [NewGroupScreen]   (CreateGroup, default mode)
       │      ├─ onCreated(newGroupId) ────► [EmptyScrapbookHistoryScreen]
       │      │                              (ScrapbookViewer empty state, picker popped)
       │      └─ onBack() ─────────────────► back
       │
       ├─► [InviteNewMemberScreen]    (CreateGroup, invite mode)
       │      ├─ onInvite() ───────────────► back to [GroupDetailScreen]
       │      └─ onBack() ─────────────────► back
       │
       ├─► [GroupDetailScreen]
       │      ├─ onOpenAllMembers() ───────► [GroupMembersScreen]
       │      ├─ onOpenMemberProfile(uid) ─► [MemberProfileScreen]   (thumbnail tap)
       │      ├─ onInviteMember() ─────────► [InviteNewMemberScreen]
       │      ├─ onOpenScrapbook(m, y) ────► [ScrapbookHistryScreen]
       │      └─ onLeaveGroup() ───────────► back (top-bar icon / red "Leave group")
       │
       ├─► [GroupMembersScreen]
       │      └─ onOpenMemberProfile(uid) ─► [MemberProfileScreen]
       │
       ├─► [MemoriesScreen]                                ◄── bottom-nav tab
       │      └─ onOpenScrapbook(g, m, y) ─► [ScrapbookHistryScreen]
       │
       ├─► [FriendsScreen]                                 ◄── bottom-nav tab
       │      ├─ onOpenSearch() ───────────► [FriendsSearchScreen]
       │      ├─ onOpenAddFriend() ────────► [AddFriendScreen]
       │      ├─ onOpenAllRequests() ──────► [AllFriendRequestsScreen]
       │      ├─ onOpenMemberProfile(uid) ─► [MemberProfileScreen]
       │      └─ onOpenGroupDetail(gid) ───► [GroupDetailScreen]
       │
       ├─► [FriendsSearchScreen]
       │      ├─ onOpenMemberProfile(uid) ─► [MemberProfileScreen]
       │      └─ onOpenGroupDetail(gid) ───► [GroupDetailScreen]
       │
       ├─► [AddFriendScreen]
       │      └─ onOpenSearch() ───────────► [AddFriendSearchScreen]
       │
       ├─► [AddFriendSearchScreen]
       │      └─ onOpenMemberProfile(uid) ─► [MemberProfileScreen]
       │
       ├─► [AllFriendRequestsScreen]
       │      └─ onOpenMemberProfile(uid) ─► [MemberProfileScreen]
       │
       ├─► [MemberProfileScreen]
       │      └─ onBack() ─────────────────► back
       │
       ├─► [ProfileScreen]                                 ◄── bottom-nav tab
       │      ├─ onOpenEditProfile() ──────► [EditProfileScreen]
       │      └─ onOpenSettings() ─────────► [SettingScreen]
       │
       ├─► [EditProfileScreen]
       │      └─ onOpenAvatarViewer() ─────► [AvatarViewerScreen]
       │
       ├─► [SettingScreen]
       │      ├─ onOpenProfile() ──────────► [EditProfileScreen]
       │      ├─ onOpenNotificationSettings() ─► [NotificationSettingsScreen]
       │      └─ onLogout() ───────────────► [LoginScreen]   (entire stack cleared)
       │
       └─► [NotificationSettingsScreen]
              └─ onBack() ─────────────────► back
```

The four bottom-nav destinations — Home, Memories, Friends, Profile — share a
single bottom bar. Tab switches use `popUpTo(start) { saveState = true }`
together with `launchSingleTop` and `restoreState`, so each tab keeps its own
scroll position and the back stack stays shallow.

---

## Project layout

```
app/src/main/java/com/cs5520group15/memorycircle/
├── data/            Repositories + mock data sources
├── model/           Plain data classes (Profile, Friend, Member, …)
└── ui/
    ├── navigation/  Destinations + NavHost
    ├── common/      Shared composables (buttons, rows, dialogs, …)
    ├── theme/       Colors, typography
    └── <feature>/   One folder per screen, each with Screen + ViewModel
```

Every screen above lives in its own `ui/<feature>/` package, paired with a
ViewModel where state is non-trivial. Repositories under `data/` back the
ViewModels; pure data classes live under `model/`.

---

## Running

Open the project in Android Studio (Giraffe or newer), let Gradle sync, then
run the `app` configuration on an emulator or physical device. The app starts
at LoginScreen — create an account via Register or sign in to land on Home.
