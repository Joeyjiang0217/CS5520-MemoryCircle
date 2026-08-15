# MemoryCircle

A collaborative Android app for small friend and family groups to build a shared
monthly **scrapbook** of photos. Members contribute time points — a photo plus a
caption — to the current month's scrapbook, browse every past month, and manage
their friend graph and group memberships from a single navigation hub.

Built with **Kotlin + Jetpack Compose**, backed by **Firebase Auth, Cloud
Firestore, and Cloud Storage**.

---

## Team & my contributions

This was a **2-person team project** (Northeastern CS5520, June 2026 — 58 commits
over 12 days). This repository is my copy of the team codebase.

| Contributor | Main areas |
| --- | --- |
| **[@Joeyjiang0217](https://github.com/Joeyjiang0217)** (me) | Firebase data layer, security rules, Friends / Groups / Profile features, notification engine |
| [@Rena-jin](https://github.com/Rena-jin) | Design system, splash animation, app chrome, first-pass mock-data screens |

**What I built** (35 of 58 commits; **13,050 of the 16,187 Kotlin lines surviving
at HEAD, ~81%**, measured by `git blame`):

- **The entire Firebase data layer** — 9 repositories (`Auth`, `Friends`,
  `FriendsSearch`, `Group`, `Profile`, `Scrapbook`, `Notifications`,
  `NotificationSettings`, `Seed`) plus the Firebase module wiring, a typed
  `Result` wrapper, and network-state handling
- **The six-phase migration** from mock data sources to live Firestore, done
  screen group by screen group so the app stayed runnable throughout
- **[`firestore.rules`](firestore.rules)** — 198 lines of authorization covering
  the full nested document tree, including the asymmetric friend-request rules
  that stop one user from writing themselves into someone else's friend list
- **Three feature verticals end to end** — Friends (search, requests, the
  accept/decline state machine), Groups (creation, membership, invites, the
  scrapbook timeline), and Profile (view, edit, avatar upload to Cloud Storage)
- **The realtime notification engine** — Firestore listeners that raise local
  system notifications, with per-channel user preferences
- **This README**

**What I did not build** — all of the following is [@Rena-jin](https://github.com/Rena-jin)'s:
the design system (colour palette, typography, theming), the launch-screen
animation, the bottom navigation bar and top app bar, the first mock-data
versions of the Home / Login / Register / Scrapbook / Memories screens, the
project-level KDoc pass, and the two test stubs.

---

## Screens

The app has **22 screens**, all Jetpack Compose — there are **zero XML layouts**
and **zero Java files** in the project.

### Auth

`LoginScreen` · `RegisterScreen`

The app launches on **Login**. Existing users sign in with email and password;
**Create Account** opens the registration form. On success the back stack is
cleared up to Login, so the system Back button never returns the user to a form
they have already completed.

<img src="ui%20screens/loginScreen.png" width="240" alt="Login screen" /> <img src="ui%20screens/registerScreen.png" width="240" alt="Register screen" />

### Home & groups

`HomeScreen` · `NewGroupScreen` · `ScrapbookViewerScreen` · `ScrapbookScreen`

**Home** lists every group the user belongs to, with a "+" FAB that opens the
contact picker for a new group.

<img src="ui%20screens/homeScreen.png" width="240" alt="Home screen listing the user's groups" /> <img src="ui%20screens/newGroupScreen.png" width="240" alt="New group contact picker" />

Opening a group lands on **ScrapbookViewer** — that group's live collaborative
timeline for the current month, where each row is one time point contributed by
one member. Both "Add photo" on an existing entry and the "+" FAB open
**ScrapbookScreen**, the single time-point editor; the difference is whether the
new photo joins an existing time point or creates a new one.

<img src="ui%20screens/scrapbookViewerScreen.png" width="240" alt="Group scrapbook timeline for the current month" /> <img src="ui%20screens/scrapbookScreen.png" width="240" alt="Time point editor with photo and caption" />

`GroupDetailScreen` · `GroupMembersScreen` · `InviteNewMemberScreen`

The hamburger icon opens **GroupDetail**, the group's settings and history hub:
a member thumbnail grid, an invite action, the per-month scrapbook list, and two
separate routes out of the group. **GroupMembers** is the full alphabetical
roster; tapping any member opens their read-only profile.

<img src="ui%20screens/groupDetailScreen.png" width="240" alt="Group detail with members and month history" /> <img src="ui%20screens/groupMembersScreen.png" width="240" alt="Full group member roster" />

**InviteNewMember** is the create-group contact picker re-used in invite mode —
the title and CTA flip, and the confirm action wires the selected users into the
existing group instead of minting a new one. A brand-new group has no records
yet, so it renders the empty state.

<img src="ui%20screens/inviteNewMemberScreen.png" width="240" alt="Invite new member picker" /> <img src="ui%20screens/emptyScrapbookHistoryScreen.png" width="240" alt="Empty state for a group with no memories yet" />

### Memories

`MemoriesScreen` · `ScrapbookHistryScreen`

**Memories** is a read-only calendar of past months across *all* of the user's
groups, rendered as monthly cover thumbnails. Tapping a month opens
**ScrapbookHistry** for that group, month, and year.

The history screen is deliberately a reduced version of the live viewer: no "Add
photo", no FAB, no menu icon. Past months are records, so only viewing is
supported.

<img src="ui%20screens/memoriesScreen.png" width="240" alt="Memories calendar of past months" /> <img src="ui%20screens/scrapbookHistryScreen.png" width="240" alt="Read-only past-month scrapbook" />

### Friends

`FriendsScreen` · `FriendsSearchScreen` · `AddFriendScreen` · `AddFriendSearchScreen` · `AllFriendRequestsScreen` · `MemberProfileScreen`

**Friends** is the friend-graph hub — it lists both friends and every group the
user belongs to. Its hero search bar opens a full-screen overlay that fuzzy
matches friends by name or email *and* groups by name.

<img src="ui%20screens/FriendsScreen.png" width="240" alt="Friends hub" /> <img src="ui%20screens/friendsSearchScreen.png" width="240" alt="Combined friend and group search overlay" />

Adding a friend is a separate two-step flow: **AddFriend** holds a tap-only
search bar, which opens **AddFriendSearch**, an active overlay with an
auto-focused field that searches people only.

<img src="ui%20screens/addFriendScreen.png" width="240" alt="Add friend landing screen" /> <img src="ui%20screens/addFriendSearchScreen.png" width="240" alt="Active people search" />

**AllFriendRequests** shows every request in all three states — pending,
accepted, declined — with per-row swipe-to-dismiss. Any user row anywhere in the
app leads to the same read-only **MemberProfile**.

<img src="ui%20screens/allFriendRequestsScreen.png" width="240" alt="All friend requests with per-row dismiss" /> <img src="ui%20screens/memberProfileScreen.png" width="240" alt="Read-only member profile" />

### Profile & settings

`ProfileScreen` · `EditProfileScreen` · `SettingScreen` · `NotificationSettingsScreen`

**Profile** is the user's own card; **EditProfile** is a tap-to-edit form for
avatar, name, bio, and email, with the avatar uploading to Cloud Storage.

<img src="ui%20screens/profileScreen.png" width="240" alt="Own profile card" /> <img src="ui%20screens/editProfileScreen.png" width="240" alt="Edit profile form" />

**Settings** is the account hub, and **NotificationSettings** holds the
per-channel toggles that the notification engine reads before raising anything.

<img src="ui%20screens/settingScreen.png" width="240" alt="Settings hub" /> <img src="ui%20screens/notificationSettingsScreen.png" width="240" alt="Per-channel notification toggles" />

---

## Features

- 📸 **Shared monthly scrapbooks** — every group gets one scrapbook per month,
  built collaboratively from member-contributed photo + caption time points
- 🗓 **Month history** — past scrapbooks stay browsable per group, and across all
  groups from the Memories calendar
- 👥 **Friend graph** — search by name or email, request / accept / decline, with
  the full request history kept
- 🏠 **Groups** — create, invite, browse the roster, leave; ownership decides who
  can delete
- 🔔 **Realtime alerts** — Firestore listeners raise local system notifications
  for requests, invites, posts, photos, comments, and new members
- 🖼 **Cloud-backed media** — photos and avatars upload to Firebase Cloud Storage
- 🧭 **State-preserving tabs** — each of the four tabs keeps its own scroll
  position and back stack

---

## Tech stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin 2.2.10 — 16,178 lines across 88 files, **0 Java** |
| UI | **Jetpack Compose only** (Compose BOM 2026.02.01, Material 3) — 219 `@Composable` functions, **0 XML layouts** |
| Navigation | Navigation Compose 2.9.0 — one `NavHost`, 22 destinations |
| Architecture | MVVM — 17 ViewModels over 9 repositories |
| Auth | Firebase Authentication (email / password) |
| Database | Cloud Firestore, with realtime snapshot listeners |
| Media | Firebase Cloud Storage · Coil 2.6.0 for image loading |
| Notifications | `NotificationCompat` — **local only**, see below |
| Build | AGP 9.2.1 · Gradle Kotlin DSL · Firebase BOM 34.14.1 |
| SDK | `compileSdk` 37 · `targetSdk` 36 · `minSdk` 28 (Android 9) |

---

## Architecture

```text
app/src/main/java/com/cs5520group15/memorycircle/
├── data/            9 repositories + FirebaseModule, Result, NetworkUtil,
│                    NotificationService
├── model/           Plain data classes (Profile, Friend, Member, Scrapbook,
│                    NotificationSettings)
└── ui/
    ├── navigation/  Destinations + the single NavHost
    ├── common/      Shared composables (buttons, rows, dialogs)
    ├── theme/       Colours, typography
    └── <feature>/   One package per screen — Screen + ViewModel
```

Screens never touch Firebase directly. State flows one way:

```text
Composable  →  ViewModel  →  Repository  →  Firebase SDK  →  Firestore / Storage
```

A few things worth calling out:

- **Repositories return a typed `Result`.** `data/Result.kt` gives every call one
  success/failure shape, so ViewModels branch on one type instead of catching
  Firebase exceptions at the UI layer.
- **Realtime by default.** Most reads are Firestore snapshot listeners rather
  than one-shot gets, so a change made on one device shows up on another without
  a refresh — this is what makes the shared scrapbook feel collaborative.
- **Notifications are local.** `NotificationService` raises `NotificationCompat`
  notifications from those same Firestore listeners. There is **no
  `firebase-messaging` dependency and no FCM** anywhere in the project — nothing
  is delivered while the app is not running.
- **One picker, two modes.** The create-group contact picker and the invite
  picker are the same composable behind a mode flag.
- **Tab state survives switching.** The four bottom-nav destinations use
  `popUpTo(start) { saveState = true }` with `launchSingleTop` and
  `restoreState`, keeping the back stack shallow.
- **A `devtools` package** seeds test data and simulates inbound activity. It is
  development scaffolding for demoing the realtime paths, not a user feature.

---

## Data model

Cloud Firestore, modelled as a nested document tree rather than flat
collections — the nesting is what lets the security rules express "members of
this group, and only them" in one place.

```text
users/{uid}
├── friends/{friendUid}
├── incomingRequests/{fromUid}
└── outgoingRequests/{toUid}

groups/{gid}
├── members/{memberUid}
└── scrapbooks/{sid}
    └── posts/{pid}
        └── comments/{cid}
```

| Collection | Contents |
| --- | --- |
| `users/{uid}` | Display name, email, bio, avatar URL |
| `users/{uid}/friends/{friendUid}` | One document per confirmed friendship, written on both sides |
| `users/{uid}/incomingRequests/{fromUid}` | Pending / accepted / declined requests received |
| `users/{uid}/outgoingRequests/{toUid}` | The mirror of the above, for requests sent |
| `groups/{gid}` | Group name, owner, `memberIds` array used by the read rule |
| `groups/{gid}/members/{memberUid}` | Per-member record within a group |
| `groups/{gid}/scrapbooks/{sid}` | One scrapbook per group per month |
| `.../scrapbooks/{sid}/posts/{pid}` | One time point — caption plus one or more photos |
| `.../posts/{pid}/comments/{cid}` | Comments on a time point |

### Security rules

[`firestore.rules`](firestore.rules) is 198 lines built on six helper functions —
`isAuth`, `isMe`, `isDeveloper`, `groupDoc`, `isGroupMember`, `isGroupOwner`.

Group content inherits one rule: you can read and write a group's members,
scrapbooks, posts, and comments **iff** you are a member of that group, and only
the owner can delete it. Friend requests are the interesting case — the rules are
deliberately asymmetric, so that accepting a request cannot be used by one user
to write themselves into another user's friend list.

---

## Getting started

Requires **Android Studio** (Giraffe or newer) and **JDK 17**.

This project needs a Firebase backend of your own — the committed
`google-services.json` points at the original course project, which you will not
have access to:

1. Create a Firebase project, and add an Android app with the package name
   `com.cs5520group15.memorycircle`.
2. Enable **Authentication** (Email/Password), **Cloud Firestore**, and **Cloud
   Storage**.
3. Download your own `google-services.json` into `app/`, replacing the committed
   one.
4. Deploy the rules: `firebase deploy --only firestore:rules`.
5. Put your own Firebase UID into the `isDeveloper()` allowlist in
   `firestore.rules` if you want the Dev Tools actions.

Then open the project, let Gradle sync, and run the `app` configuration on an
emulator or device. Register an account from the app to reach Home.

---
