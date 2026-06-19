package com.cs5520group15.memorycircle.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

/**
 * Handles all Firebase Authentication operations: register, login, password
 * reset, logout, and current-user accessors. ViewModels call these suspend
 * functions and branch on the returned Result.
 *
 * Singleton — there's only ever one Firebase Auth state for the device.
 */
object AuthRepository {

    private val auth = FirebaseModule.auth
    private val db   = FirebaseModule.db

    // ── Current-user helpers ─────────────────────────────────────────────────

    /** uid of whoever is logged in right now, or null. */
    val currentUid: String?
        get() = auth.currentUser?.uid

    /** Full FirebaseUser (uid, email, displayName, ...), or null. */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // ── Register ─────────────────────────────────────────────────────────────

    /**
     * Creates a Firebase Auth account with email + password, then writes a
     * matching users/{uid} document with the display name.
     */
    suspend fun register(name: String, email: String, password: String): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user!!.uid

            val userDoc = mapOf(
                "uid"       to uid,
                "name"      to name,
                "email"     to email,
                "bio"       to "",
                "avatarUrl" to "",
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(userDoc).await()

            Result.Success(uid)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Registration failed")
        }
    }

    // ── Login ────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            Result.Success(authResult.user!!.uid)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Login failed")
        }
    }

    // ── Password reset ───────────────────────────────────────────────────────

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to send reset email")
        }
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    fun logout() {
        auth.signOut()
    }

    // ── Profile name lookup ──────────────────────────────────────────────────

    /**
     * What: A user's render-time identity — display name and avatar download
     *       URL — surfaced together so callers (group members list, post
     *       author rows, comment rows) get both fields in one Firestore round
     *       trip instead of querying for them separately.
     */
    data class UserBrief(val name: String, val avatarUrl: String)

    /**
     * Process-wide cache of uid -> (name, avatarUrl). Avoids re-reading
     * users/{uid} every time we need to render an author / member row.
     * Cleared automatically when the process dies; renames or avatar uploads
     * during a session surface lazily as new lookups happen — the editor's own
     * profile updates instantly via ProfileRepository's live listener.
     */
    private val briefCache = mutableMapOf<String, UserBrief>()

    /**
     * Reads the current user's display name from Firestore.
     * Firebase Auth doesn't store name natively, so we read from users/{uid}.
     */
    suspend fun getCurrentUserName(): Result<String> {
        return try {
            val uid = currentUid ?: return Result.Error("Not logged in")
            val name = getUserName(uid) ?: "User"
            Result.Success(name)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get user name")
        }
    }

    /**
     * What: Single-uid display-name lookup. Hits the in-memory brief cache
     *       first; on miss reads users/{uid} (both name + avatarUrl in one
     *       round trip) and caches the result.
     * Who:  Used wherever only the name is needed (HomeScreen greeting,
     *       CreateGroup owner member subdoc seed).
     */
    suspend fun getUserName(uid: String): String? {
        briefCache[uid]?.let { return it.name }
        return try {
            val doc = db.collection("users").document(uid).get().await()
            val name = doc.getString("name") ?: return null
            briefCache[uid] = UserBrief(
                name      = name,
                avatarUrl = doc.getString("avatarUrl").orEmpty()
            )
            name
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Batched display-name lookup. Thin projection over [getUserBriefs] — same
     * Firestore round trip, just drops the avatar field. Existing callers
     * (ScrapbookRepository.assemble, etc.) keep their signature.
     */
    suspend fun getUserNames(uids: List<String>): Map<String, String> =
        getUserBriefs(uids).mapValues { it.value.name }

    /**
     * What: Batched (uid → name + avatarUrl) lookup. Returns a map covering
     *       every input id whose users doc exists; missing/unknown ids are
     *       absent. Uncached ids are fetched in chunks of 30 (Firestore's
     *       whereIn limit) via whereIn(documentId(), …) and added to the
     *       cache.
     * Who:  Used by GroupDetailViewModel / GroupMembersViewModel to render
     *       member avatars, and by ScrapbookRepository.assemble() to attach
     *       avatars to post / comment / photo authors at read time.
     * When: One call per snapshot refresh on the consumer; the cache absorbs
     *       repeated lookups so a timeline of N posts costs at most
     *       ceil(uniqueAuthors / 30) round trips.
     */
    suspend fun getUserBriefs(uids: List<String>): Map<String, UserBrief> {
        val unique = uids.toSet()
        val result = mutableMapOf<String, UserBrief>()
        val missing = mutableListOf<String>()

        unique.forEach { uid ->
            val cached = briefCache[uid]
            if (cached != null) result[uid] = cached else missing += uid
        }

        if (missing.isEmpty()) return result

        missing.chunked(30).forEach { chunk ->
            try {
                val snap = db.collection("users")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get().await()
                snap.documents.forEach { doc ->
                    val name = doc.getString("name") ?: return@forEach
                    val brief = UserBrief(
                        name      = name,
                        avatarUrl = doc.getString("avatarUrl").orEmpty()
                    )
                    briefCache[doc.id] = brief
                    result[doc.id] = brief
                }
            } catch (_: Exception) {
                // Best-effort: missing entries simply don't appear in the map.
            }
        }
        return result
    }
}
