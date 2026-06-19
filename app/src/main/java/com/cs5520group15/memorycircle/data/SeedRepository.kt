package com.cs5520group15.memorycircle.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

/**
 * What: Debug-only helper that pre-populates Firestore so the team can drive
 *       the UI without typing in test data by hand each session.
 *
 *       Three idempotent operations:
 *         1) seedTestUsers()  — register 1@test.com .. 10@test.com (pwd "123456")
 *            on a separate FirebaseApp instance so the caller stays logged in
 *            on the primary app.
 *         2) seedTestPost()   — append one post to the current month's scrapbook
 *            for the current user's first group (or no-op if they have none).
 *         3) seedHistoricalScrapbooks() — create three past-month scrapbooks
 *            (T-1, T-2, T-3) for that same group, each with two seeded posts.
 *
 *       All writes use FieldValue.serverTimestamp() where applicable so the
 *       data shape exactly matches what the production write paths produce.
 *
 * Who: Called by the Dev Tools screen.
 * When: Tapped manually by a developer while logged in.
 */
object SeedRepository {

    private val db: FirebaseFirestore get() = FirebaseModule.db

    data class SeedReport(val createdUsers: Int, val skippedUsers: Int, val errors: List<String>)

    /**
     * Registers users 1@test.com .. 10@test.com (password "123456") on a
     * SECONDARY FirebaseApp instance so the primary session — the developer
     * who's running this — isn't signed out by `createUserWithEmailAndPassword`.
     *
     * After each Auth account is minted we also write a matching users/{uid}
     * Firestore doc (name = "Test User N") so the rest of the app can resolve
     * display names without dropping back to "User".
     */
    suspend fun seedTestUsers(appContext: Context, count: Int = 10): SeedReport {
        val secondary = secondaryApp(appContext)
        val auth = FirebaseAuth.getInstance(secondary)
        val errors  = mutableListOf<String>()
        var created = 0
        var skipped = 0

        for (i in 1..count) {
            val email = "$i@test.com"
            val password = "123456"
            val name = "Test User $i"
            try {
                val res = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = res.user!!.uid
                db.collection("users").document(uid).set(mapOf(
                    "uid"       to uid,
                    "name"      to name,
                    "email"     to email,
                    "bio"       to "",
                    "avatarUrl" to "",
                    "createdAt" to FieldValue.serverTimestamp()
                )).await()
                auth.signOut()
                created++
            } catch (e: Exception) {
                // Most likely cause is "email already in use" — counts as skipped.
                val msg = e.message ?: ""
                if (msg.contains("already in use", ignoreCase = true)) skipped++
                else errors += "$email: $msg"
            }
        }

        return SeedReport(created, skipped, errors)
    }

    /**
     * Adds a single seeded post to the current month's scrapbook of the
     * current user's first group. Returns the post id, or null with a reason
     * if there's nothing to write to.
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

    // ── Helpers ─────────────────────────────────────────────────────────────

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
