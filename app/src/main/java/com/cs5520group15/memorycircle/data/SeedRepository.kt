package com.cs5520group15.memorycircle.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

/**
 * What: Debug-only helper that pre-populates Firestore so the team can drive
 *       the UI without typing in test data by hand each session.
 *
 *       Idempotent operations behind the Dev Tools screen:
 *         1) seedTestUsers()             — register 1@test.com .. 10@test.com.
 *         2) seedTestPost()              — append one post to the current
 *                                          month's scrapbook for the user's
 *                                          first group.
 *         3) seedHistoricalScrapbooks()  — create three past-month scrapbooks.
 *         4) seedFriendships()           — befriend 1@..5@test.com.
 *         5) seedTestUserProfiles()      — patch themed bio + avatar onto the
 *                                          test users, ensuring the new public
 *                                          shape (emailMasked, no `email`).
 *         6) clearTestUserProfiles()     — blank bio + avatar on the test
 *                                          users so the demo can show the
 *                                          "fresh / undecorated" state.
 *
 *       Every write that touches name or avatarUrl also fans out to every
 *       group the target user belongs to via groups/{gid}/members/{uid}, so
 *       group screens reflect the change without a restart.
 *
 * Who: Called by the Dev Tools screen.
 * When: Tapped manually by a developer while logged in.
 */
object SeedRepository {

    private val db: FirebaseFirestore get() = FirebaseModule.db

    data class SeedReport(
        val createdUsers:    Int,
        val skippedUsers:    Int,
        val backfilledUsers: Int,
        val errors:          List<String>
    )

    /**
     * Registers users 1@test.com .. 10@test.com (password "123456") on a
     * SECONDARY FirebaseApp instance so the primary session — the developer
     * who's running this — isn't signed out by `createUserWithEmailAndPassword`.
     *
     * After each Auth account is minted we also write a matching users/{uid}
     * Firestore doc using the new public-only shape: name, bio, avatarUrl,
     * emailMasked (full email lives only in Auth).
     */
    suspend fun seedTestUsers(appContext: Context, count: Int = 10): SeedReport {
        val secondary = secondaryApp(appContext)
        val auth = FirebaseAuth.getInstance(secondary)
        val errors     = mutableListOf<String>()
        var created    = 0
        var skipped    = 0
        var backfilled = 0

        for (i in 1..count) {
            val email = "$i@test.com"
            val password = "123456"
            val name = "Test User $i"
            try {
                val res = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = res.user!!.uid
                db.collection("users").document(uid).set(mapOf(
                    "uid"         to uid,
                    "name"        to name,
                    "nameLower"   to AuthRepository.nameLower(name),
                    "emailMasked" to AuthRepository.maskEmail(email),
                    "emailHash"   to AuthRepository.emailHash(email),
                    "bio"         to "",
                    "avatarUrl"   to "",
                    "createdAt"   to FieldValue.serverTimestamp()
                )).await()
                auth.signOut()
                created++
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("already in use", ignoreCase = true)) {
                    // Auth account exists from a prior seed run. The users
                    // doc was likely written by an older build that
                    // didn't know about nameLower / emailHash, so name
                    // searches (orderBy "nameLower") would silently drop
                    // it. Resolve the uid by display name (the only key
                    // stable across schema versions) and merge the
                    // missing indexed fields in.
                    val patched = runCatching {
                        val existingUid = lookupUidsByName(listOf(name))[name]
                            ?: return@runCatching false
                        db.collection("users").document(existingUid).set(mapOf(
                            "nameLower"   to AuthRepository.nameLower(name),
                            "emailMasked" to AuthRepository.maskEmail(email),
                            "emailHash"   to AuthRepository.emailHash(email)
                        ), SetOptions.merge()).await()
                        true
                    }.getOrDefault(false)
                    if (patched) backfilled++
                    skipped++
                } else errors += "$email: $msg"
            }
        }

        return SeedReport(created, skipped, backfilled, errors)
    }

    /**
     * Adds a single seeded post to the current month's scrapbook of the
     * current user's first group. Returns the post id, or throws with a
     * reason if there's nothing to write to.
     */
    suspend fun seedTestPost(): String {
        val uid = AuthRepository.currentUid ?: error("Not logged in")
        val groupId = firstGroupIdFor(uid) ?: error("Current user is not in any group yet — create one first")

        val scrapbookId = YearMonth.now().toString()
        ensureScrapbookDoc(groupId, scrapbookId)

        val postRef = db.collection("groups").document(groupId)
            .collection("scrapbooks").document(scrapbookId)
            .collection("posts").document()
        val postId = postRef.id

        val today = LocalDate.now()
        val ts = Timestamp(today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)

        postRef.set(mapOf(
            "postId"       to postId,
            "authorId"     to uid,
            "title"        to "Seeded Test Post",
            "date"         to ts,
            "tags"         to listOf("#seed", "#test"),
            "photos"       to listOf(mapOf(
                "photoId"     to UUID.randomUUID().toString(),
                "url"         to "https://picsum.photos/seed/${postId}/600/400",
                "storagePath" to "",
                "description" to "This is a seeded test post (no real upload).",
                "uploaderId"  to uid,
                "uploadedAt"  to Timestamp.now()
            )),
            "commentCount" to 0,
            "createdAt"    to FieldValue.serverTimestamp()
        )).await()

        db.collection("groups").document(groupId)
            .collection("scrapbooks").document(scrapbookId)
            .update("postCount", FieldValue.increment(1)).await()
        db.collection("groups").document(groupId)
            .update("memoryCount", FieldValue.increment(1)).await()

        return postId
    }

    /**
     * Creates three historical scrapbooks (one per past month T-1..T-3) for
     * the current user's first group, each pre-populated with two seeded posts.
     * Skips any month doc that already exists so re-running is safe.
     */
    suspend fun seedHistoricalScrapbooks(): Int {
        val uid = AuthRepository.currentUid ?: error("Not logged in")
        val groupId = firstGroupIdFor(uid) ?: error("Current user is not in any group yet — create one first")

        val now = YearMonth.now()
        var written = 0

        for (offset in 1..3) {
            val ym = now.minusMonths(offset.toLong())
            val sbId = ym.toString()
            val sbRef = db.collection("groups").document(groupId)
                .collection("scrapbooks").document(sbId)

            val exists = sbRef.get().await().exists()
            if (exists) continue

            sbRef.set(mapOf(
                "scrapbookId" to sbId,
                "postCount"   to 2,
                "createdAt"   to FieldValue.serverTimestamp(),
                "updatedAt"   to FieldValue.serverTimestamp()
            )).await()

            // Two posts per historical month, spaced two weeks apart.
            listOf(5, 19).forEachIndexed { idx, day ->
                val date = ym.atDay(day.coerceAtMost(ym.lengthOfMonth()))
                val ts = Timestamp(date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)
                val postRef = sbRef.collection("posts").document()
                val postId = postRef.id
                postRef.set(mapOf(
                    "postId"       to postId,
                    "authorId"     to uid,
                    "title"        to "Memory from ${ym.month.name.lowercase().replaceFirstChar { it.uppercase() }} #${idx + 1}",
                    "date"         to ts,
                    "tags"         to listOf("#${ym.month.name.lowercase()}"),
                    "photos"       to listOf(mapOf(
                        "photoId"     to UUID.randomUUID().toString(),
                        "url"         to "https://picsum.photos/seed/${sbId}_${idx}/600/400",
                        "storagePath" to "",
                        "description" to "Historical seed for $sbId",
                        "uploaderId"  to uid,
                        "uploadedAt"  to Timestamp.now()
                    )),
                    "commentCount" to 0,
                    "createdAt"    to FieldValue.serverTimestamp()
                )).await()
            }

            written++
        }

        if (written > 0) {
            db.collection("groups").document(groupId)
                .update("memoryCount", FieldValue.increment((written * 2).toLong())).await()
        }
        return written
    }

    /**
     * Befriends the currently signed-in user with 1@test.com .. 5@test.com.
     * Writes BOTH sides of each friendship — users/{me}/friends/{friend} AND
     * users/{friend}/friends/{me} — so the lookups stay symmetric without a
     * Cloud Function. Returns how many new friendships were created (skipping
     * pairs that already exist).
     *
     * 6@..10@test.com are intentionally left out so you can exercise the
     * add-friend flow against them.
     */
    suspend fun seedFriendships(targetIndices: IntRange = 1..5): Int {
        val me = AuthRepository.currentUid ?: error("Not logged in")

        val names = targetIndices.map { "Test User $it" }
        val byName = lookupUidsByName(names)
        val targetUids = names.mapNotNull { byName[it] }
        if (targetUids.isEmpty()) {
            error("Found 0 of the target test users — run 'Seed users' first")
        }

        var created = 0
        targetUids.forEach { friendUid ->
            if (friendUid == me) return@forEach
            val myRef    = db.collection("users").document(me).collection("friends").document(friendUid)
            val theirRef = db.collection("users").document(friendUid).collection("friends").document(me)
            val already  = myRef.get().await().exists()
            if (already) return@forEach
            myRef.set(mapOf(
                "uid"   to friendUid,
                "since" to FieldValue.serverTimestamp()
            )).await()
            theirRef.set(mapOf(
                "uid"   to me,
                "since" to FieldValue.serverTimestamp()
            )).await()
            created++
        }
        return created
    }

    /**
     * For users 1@test.com .. 10@test.com (whichever exist), sets a themed bio
     * + a stable picsum avatar URL so the demo screens don't all show the same
     * Sage letter blob. Also upgrades any old-shape doc to the new public
     * shape: writes `emailMasked` and removes the leftover `email` field.
     *
     * Every avatar/name change is fanned out to groups/{gid}/members/{uid} so
     * member listings on other devices refresh without a restart, exactly the
     * same pattern ProfileRepository uses for self-edits.
     */
    suspend fun seedTestUserProfiles(targetIndices: IntRange = 1..10): Int {
        val names = targetIndices.map { "Test User $it" }
        val byName = lookupUidsByName(names)

        var patched = 0
        names.forEach { n ->
            val uid = byName[n] ?: return@forEach
            val number = n.substringAfterLast(' ')
            val email     = "$number@test.com"
            val bio       = "I'm $n — auto-seeded from Dev Tools."
            val avatarUrl = "https://picsum.photos/seed/testuser_$number/200/200"

            db.collection("users").document(uid).set(mapOf(
                "bio"         to bio,
                "avatarUrl"   to avatarUrl,
                "nameLower"   to AuthRepository.nameLower(n),
                "emailMasked" to AuthRepository.maskEmail(email),
                "emailHash"   to AuthRepository.emailHash(email),
                "email"       to FieldValue.delete()
            ), SetOptions.merge()).await()

            fanOutMemberCard(uid, name = n, avatarUrl = avatarUrl, bio = bio)
            patched++
        }
        return patched
    }

    /**
     * Inverse of seedTestUserProfiles: blanks bio + avatarUrl on the test
     * users so screens fall back to the letter-avatar state. Useful for
     * demoing "before any user has uploaded a picture", or for confirming the
     * cross-device update path by running clear → seed and watching every
     * surface refresh without leaving the screens.
     *
     * Fans the cleared avatar/name out to each group's members subcollection
     * for the same reason as the seed path — otherwise group screens would
     * keep the previous denormalized values.
     */
    suspend fun clearTestUserProfiles(targetIndices: IntRange = 1..10): Int {
        val names = targetIndices.map { "Test User $it" }
        val byName = lookupUidsByName(names)

        var cleared = 0
        names.forEach { n ->
            val uid = byName[n] ?: return@forEach
            db.collection("users").document(uid).update(mapOf(
                "bio"       to "",
                "avatarUrl" to ""
            )).await()
            fanOutMemberCard(uid, name = n, avatarUrl = "", bio = "")
            cleared++
        }
        return cleared
    }

    /**
     * Impersonates "Test User N" accepting every PENDING incoming friend
     * request in their `incomingRequests` subcollection. Mirrors the same
     * batch shape FriendsRepository.accept uses for a real user tap:
     *   - writes both sides of the friendship
     *   - flips the incoming doc's `status` to ACCEPTED (kept so the
     *     See-all history view still shows the row — only a manual swipe
     *     delete removes it)
     *   - deletes the sender's outgoingRequests/{N} entry so AddFriendSearch
     *     on the sender's device flips "Invitation sent" → "Added".
     *
     * Already-actioned (ACCEPTED / DECLINED) docs are skipped — re-running
     * on a fully-actioned inbox is a no-op.
     */
    suspend fun acceptIncomingRequestsForTestUser(n: Int = 6): Int {
        val name = "Test User $n"
        val byName = lookupUidsByName(listOf(name))
        val targetUid = byName[name] ?: error("$name not found — run 'Seed users' first")

        val incomingSnap = db.collection("users").document(targetUid)
            .collection("incomingRequests").get().await()

        var accepted = 0
        incomingSnap.documents.forEach { reqDoc ->
            val statusStr = reqDoc.getString("status").orEmpty().uppercase()
            // Anything that isn't explicitly PENDING (or missing — treat as
            // PENDING for legacy docs written before the status field
            // existed) has already been actioned.
            if (statusStr.isNotEmpty() && statusStr != "PENDING") return@forEach

            val fromUid = reqDoc.id
            runCatching {
                val targetFriendRef = db.collection("users").document(targetUid)
                    .collection("friends").document(fromUid)
                val senderFriendRef = db.collection("users").document(fromUid)
                    .collection("friends").document(targetUid)
                val senderOutgoingRef = db.collection("users").document(fromUid)
                    .collection("outgoingRequests").document(targetUid)

                db.runBatch { batch ->
                    batch.set(targetFriendRef, mapOf(
                        "uid"   to fromUid,
                        "since" to FieldValue.serverTimestamp()
                    ))
                    batch.set(senderFriendRef, mapOf(
                        "uid"   to targetUid,
                        "since" to FieldValue.serverTimestamp()
                    ))
                    batch.update(reqDoc.reference, mapOf(
                        "status"     to "ACCEPTED",
                        "actionedAt" to FieldValue.serverTimestamp()
                    ))
                    batch.delete(senderOutgoingRef)
                }.await()
                accepted++
            }
        }
        return accepted
    }

    // ── Notification simulation helpers ─────────────────────────────────────
    //
    // These six functions write the SAME Firestore documents a real user
    // action would write, so the live NotificationsRepository listeners on
    // the developer's device see them as genuine events and fire system
    // notifications. They're idempotent where it makes sense (the "group
    // invite" sim reuses an existing sim group instead of creating one
    // per click) so repeated taps don't pollute the data set.

    /**
     * Writes an incoming friend-request doc into MY incomingRequests as if
     * Test User N had tapped Add on me. Also creates the matching
     * outgoingRequests entry on Test User N's side so the eventual accept
     * flow works correctly. Triggers the friend-request notification.
     */
    suspend fun simulateFriendRequestFromTestUser(n: Int) {
        val me        = AuthRepository.currentUid ?: error("Not logged in")
        val senderName = "Test User $n"
        val senderUid = lookupUidsByName(listOf(senderName))[senderName]
            ?: error("$senderName not found — run 'Seed users' first")
        if (senderUid == me) error("Can't simulate a request from yourself")

        val brief = AuthRepository.getUserBriefs(listOf(senderUid))[senderUid]

        val incomingRef = db.collection("users").document(me)
            .collection("incomingRequests").document(senderUid)
        val outgoingRef = db.collection("users").document(senderUid)
            .collection("outgoingRequests").document(me)

        db.runBatch { batch ->
            batch.set(incomingRef, mapOf(
                "uid"         to senderUid,
                "name"        to (brief?.name ?: senderName),
                "avatarUrl"   to (brief?.avatarUrl.orEmpty()),
                "bio"         to (brief?.bio.orEmpty()),
                "status"      to "PENDING",
                "requestedAt" to FieldValue.serverTimestamp()
            ))
            batch.set(outgoingRef, mapOf(
                "uid"         to me,
                "requestedAt" to FieldValue.serverTimestamp()
            ))
        }.await()
    }

    /**
     * Has Test User N create a group and add me to it. Idempotent — if a
     * sim group with that owner + me as member already exists, reuses it
     * instead of spawning a new one. Triggers the group-invite notification.
     * Returns the group id (useful for the post / photo / comment sims).
     */
    suspend fun simulateGroupInviteFromTestUser(n: Int): String {
        val me         = AuthRepository.currentUid ?: error("Not logged in")
        val ownerName  = "Test User $n"
        val ownerUid   = lookupUidsByName(listOf(ownerName))[ownerName]
            ?: error("$ownerName not found — run 'Seed users' first")

        // Reuse an existing sim group if there's already one owned by N
        // with me as a member.
        val existing = db.collection("groups")
            .whereEqualTo("ownerId", ownerUid)
            .get().await()
            .documents.firstOrNull { doc ->
                @Suppress("UNCHECKED_CAST")
                val ids = (doc.get("memberIds") as? List<String>).orEmpty()
                me in ids
            }
        if (existing != null) return existing.id

        val groupRef = db.collection("groups").document()
        val gid       = groupRef.id
        val groupName = "$ownerName's Sim Group"
        val members   = listOf(ownerUid, me)

        groupRef.set(mapOf(
            "groupId"     to gid,
            "name"        to groupName,
            "createdAt"   to FieldValue.serverTimestamp(),
            "ownerId"     to ownerUid,
            "memberIds"   to members,
            "memberCount" to members.size,
            "memoryCount" to 0
        )).await()

        val briefs = AuthRepository.getUserBriefs(members)
        members.forEach { memberUid ->
            val brief = briefs[memberUid]
            groupRef.collection("members").document(memberUid).set(mapOf(
                "uid"       to memberUid,
                "name"      to (brief?.name.orEmpty()),
                "avatarUrl" to (brief?.avatarUrl.orEmpty()),
                "bio"       to (brief?.bio.orEmpty()),
                "joinedAt"  to FieldValue.serverTimestamp()
            )).await()
        }

        val scrapbookId = YearMonth.now().toString()
        groupRef.collection("scrapbooks").document(scrapbookId).set(mapOf(
            "scrapbookId" to scrapbookId,
            "postCount"   to 0,
            "createdAt"   to FieldValue.serverTimestamp(),
            "updatedAt"   to FieldValue.serverTimestamp()
        )).await()

        return gid
    }

    /**
     * Adds Test User N as a member of MY first owned group. Triggers the
     * new-member notification on the owner (me).
     */
    suspend fun simulateUserJoiningMyGroup(n: Int) {
        val me        = AuthRepository.currentUid ?: error("Not logged in")
        val joinName  = "Test User $n"
        val joinerUid = lookupUidsByName(listOf(joinName))[joinName]
            ?: error("$joinName not found — run 'Seed users' first")
        if (joinerUid == me) error("You can't join your own group as another user")

        val ownedSnap = db.collection("groups")
            .whereEqualTo("ownerId", me)
            .limit(1)
            .get().await()
        val groupDoc = ownedSnap.documents.firstOrNull()
            ?: error("You don't own any group — create one first")
        val gid = groupDoc.id

        @Suppress("UNCHECKED_CAST")
        val currentMembers = (groupDoc.get("memberIds") as? List<String>).orEmpty()
        if (joinerUid in currentMembers) error("$joinName is already in this group")

        val groupRef = db.collection("groups").document(gid)
        groupRef.update(mapOf(
            "memberIds"   to FieldValue.arrayUnion(joinerUid),
            "memberCount" to FieldValue.increment(1)
        )).await()

        val brief = AuthRepository.getUserBriefs(listOf(joinerUid))[joinerUid]
        groupRef.collection("members").document(joinerUid).set(mapOf(
            "uid"       to joinerUid,
            "name"      to (brief?.name ?: joinName),
            "avatarUrl" to (brief?.avatarUrl.orEmpty()),
            "bio"       to (brief?.bio.orEmpty()),
            "joinedAt"  to FieldValue.serverTimestamp()
        )).await()
    }

    /**
     * Inserts a new post authored by Test User N into the sim group from
     * `simulateGroupInviteFromTestUser`. Auto-creates the sim group if it
     * doesn't exist yet so the test sequence works in any order. Triggers
     * the new-post notification.
     */
    suspend fun simulateNewPostByTestUser(n: Int) {
        val gid       = ensureSimGroupWithTestUserAsOwner(n)
        val authorUid = lookupUidsByName(listOf("Test User $n"))["Test User $n"]
            ?: error("Test User $n not found")

        val scrapbookId = YearMonth.now().toString()
        val sbRef = db.collection("groups").document(gid)
            .collection("scrapbooks").document(scrapbookId)
        if (!sbRef.get().await().exists()) {
            sbRef.set(mapOf(
                "scrapbookId" to scrapbookId,
                "postCount"   to 0,
                "createdAt"   to FieldValue.serverTimestamp(),
                "updatedAt"   to FieldValue.serverTimestamp()
            )).await()
        }

        val postRef = sbRef.collection("posts").document()
        val postId  = postRef.id
        val today   = LocalDate.now()
        val date    = Timestamp(today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0)

        postRef.set(mapOf(
            "postId"       to postId,
            "authorId"     to authorUid,
            "title"        to "Post from Test User $n (sim)",
            "date"         to date,
            "tags"         to emptyList<String>(),
            "photos"       to listOf(mapOf(
                "photoId"     to UUID.randomUUID().toString(),
                "url"         to "https://picsum.photos/seed/${postId}/600/400",
                "storagePath" to "",
                "description" to "Simulated post by Test User $n",
                "uploaderId"  to authorUid,
                "uploadedAt"  to Timestamp.now()
            )),
            "commentCount" to 0,
            "createdAt"    to FieldValue.serverTimestamp()
        )).await()

        sbRef.update("postCount", FieldValue.increment(1)).await()
        db.collection("groups").document(gid)
            .update("memoryCount", FieldValue.increment(1)).await()
    }

    /**
     * Appends a new photo entry to the latest post authored by Test User N
     * in the sim group. Auto-runs the "new post" sim first if no post by
     * N exists yet, so the user can run sims out of order. Triggers the
     * new-photo notification.
     */
    suspend fun simulateNewPhotoByTestUser(n: Int) {
        val gid       = ensureSimGroupWithTestUserAsOwner(n)
        val authorUid = lookupUidsByName(listOf("Test User $n"))["Test User $n"]
            ?: error("Test User $n not found")

        val postRef = latestPostByAuthor(gid, authorUid) ?: run {
            simulateNewPostByTestUser(n)
            latestPostByAuthor(gid, authorUid)
                ?: error("Could not find or create a post by Test User $n")
        }

        val snap = postRef.get().await()
        @Suppress("UNCHECKED_CAST")
        val photos = (snap.get("photos") as? List<Map<String, Any?>>).orEmpty()
        val newPhoto = mapOf(
            "photoId"     to UUID.randomUUID().toString(),
            "url"         to "https://picsum.photos/seed/sim_${System.currentTimeMillis()}/600/400",
            "storagePath" to "",
            "description" to "Simulated new photo by Test User $n",
            "uploaderId"  to authorUid,
            "uploadedAt"  to Timestamp.now()
        )
        postRef.update("photos", photos + newPhoto).await()
    }

    /**
     * Adds a comment authored by Test User N on the latest post they
     * authored in the sim group. Auto-creates a post if none exists.
     * Triggers the new-comment notification.
     */
    suspend fun simulateCommentByTestUser(n: Int) {
        val gid       = ensureSimGroupWithTestUserAsOwner(n)
        val authorUid = lookupUidsByName(listOf("Test User $n"))["Test User $n"]
            ?: error("Test User $n not found")

        val postRef = latestPostByAuthor(gid, authorUid) ?: run {
            simulateNewPostByTestUser(n)
            latestPostByAuthor(gid, authorUid)
                ?: error("Could not find or create a post by Test User $n")
        }

        val commentRef = postRef.collection("comments").document()
        commentRef.set(mapOf(
            "commentId" to commentRef.id,
            "authorId"  to authorUid,
            "text"      to "Nice memory! (sim from Test User $n)",
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
        postRef.update("commentCount", FieldValue.increment(1)).await()
    }

    /**
     * Returns the sim group's id, creating it via the "group invite" sim
     * path if needed. Lets the post / photo / comment sims work in any
     * order — they all share the same sim group per Test User.
     */
    private suspend fun ensureSimGroupWithTestUserAsOwner(n: Int): String =
        simulateGroupInviteFromTestUser(n)

    /**
     * Returns the most-recent current-month post in `gid` whose authorId
     * matches `authorUid`, or null if none exist yet.
     */
    private suspend fun latestPostByAuthor(gid: String, authorUid: String):
            com.google.firebase.firestore.DocumentReference? {
        val scrapbookId = YearMonth.now().toString()
        val snap = db.collection("groups").document(gid)
            .collection("scrapbooks").document(scrapbookId)
            .collection("posts")
            .whereEqualTo("authorId", authorUid)
            .get().await()
        if (snap.isEmpty) return null
        val sorted = snap.documents.sortedByDescending {
            it.getTimestamp("createdAt")?.seconds ?: 0L
        }
        return sorted.first().reference
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Returns a map from display name → uid for every users-doc whose `name`
     * matches one of `names`. Used instead of email-based lookup since email
     * no longer lives in the public users doc.
     */
    private suspend fun lookupUidsByName(names: List<String>): Map<String, String> {
        if (names.isEmpty()) return emptyMap()
        val byName = mutableMapOf<String, String>()
        names.chunked(30).forEach { chunk ->
            val snap = db.collection("users").whereIn("name", chunk).get().await()
            snap.documents.forEach { doc ->
                val name = doc.getString("name") ?: return@forEach
                byName[name] = doc.id
            }
        }
        return byName
    }

    /**
     * Writes the supplied name + avatarUrl into groups/{gid}/members/{uid} for
     * every group `uid` is currently a member of. Best-effort: individual
     * failures don't surface. Used by the seed/clear flows where the target
     * user's app isn't running, so ProfileRepository.fanOutToMyGroupMembers
     * wouldn't otherwise fire for them.
     */
    private suspend fun fanOutMemberCard(uid: String, name: String, avatarUrl: String, bio: String) {
        try {
            val snap = db.collection("groups")
                .whereArrayContains("memberIds", uid)
                .get().await()
            snap.documents.forEach { doc ->
                runCatching {
                    db.collection("groups").document(doc.id)
                        .collection("members").document(uid)
                        .set(mapOf(
                            "uid"       to uid,
                            "name"      to name,
                            "avatarUrl" to avatarUrl,
                            "bio"       to bio,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ), SetOptions.merge()).await()
                }
            }
        } catch (_: Exception) {
            // Best-effort; the primary users-doc write already succeeded.
        }
    }

    private suspend fun firstGroupIdFor(uid: String): String? {
        val snap = db.collection("groups")
            .whereArrayContains("memberIds", uid)
            .limit(1)
            .get().await()
        return snap.documents.firstOrNull()?.id
    }

    /**
     * Ensures the per-month scrapbook doc exists before posts are appended to
     * its subcollection (matches the path CreateGroupViewModel uses for the
     * current month so the data shape stays consistent).
     */
    private suspend fun ensureScrapbookDoc(groupId: String, scrapbookId: String) {
        val ref = db.collection("groups").document(groupId)
            .collection("scrapbooks").document(scrapbookId)
        if (!ref.get().await().exists()) {
            ref.set(mapOf(
                "scrapbookId" to scrapbookId,
                "postCount"   to 0,
                "createdAt"   to FieldValue.serverTimestamp(),
                "updatedAt"   to FieldValue.serverTimestamp()
            )).await()
        }
    }

    /**
     * Returns (or lazily creates) the secondary FirebaseApp instance used for
     * creating Auth accounts without disturbing the primary session.
     */
    private fun secondaryApp(appContext: Context): FirebaseApp {
        val name = "memorycircle-seed"
        return runCatching { FirebaseApp.getInstance(name) }.getOrElse {
            val primary = FirebaseApp.getInstance().options
            val opts = FirebaseOptions.Builder()
                .setApiKey(primary.apiKey)
                .setApplicationId(primary.applicationId)
                .setProjectId(primary.projectId)
                .setStorageBucket(primary.storageBucket)
                .build()
            FirebaseApp.initializeApp(appContext, opts, name)
        }
    }
}
