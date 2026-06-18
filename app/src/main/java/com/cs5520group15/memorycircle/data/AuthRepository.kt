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
     * Process-wide cache of uid -> display name. Avoids re-reading users/{uid}
     * every time we need to render a post's authorName or a comment's authorName.
     * Cleared automatically when the process dies; renames during a session will
     * surface lazily as new lookups happen.
     */
    private val nameCache = mutableMapOf<String, String>()

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
     * What: Single-uid display-name lookup. Hits the in-memory cache first; on
     *       miss reads `users/{uid}.name` and caches the result.
     * Who:  Used by the scrapbook layer to enrich post/comment authorIds with
     *       display names at render time.
     */
    suspend fun getUserName(uid: String): String? {
        nameCache[uid]?.let { return it }
        return try {
            val doc = db.collection("users").document(uid).get().await()
            val name = doc.getString("name")
            if (name != null) nameCache[uid] = name
            name
        } catch (e: Exception) {
            null
        }
    }

    /**
     * What: Batched display-name lookup. Returns a map uid -> name covering
     *       every input id; missing/unknown ids are absent from the map.
     *       Uncached ids are fetched in chunks of 30 (Firestore's whereIn
     *       limit) via `whereIn(documentId(), …)` and added to the cache.
     * Who:  Used by ScrapbookRepository.assemble() so a timeline of N posts
     *       costs at most ceil(uniqueAuthors / 30) Firestore round-trips
     *       instead of N individual reads.
     */
    suspend fun getUserNames(uids: List<String>): Map<String, String> {
        val unique = uids.toSet()
        val result = mutableMapOf<String, String>()
        val missing = mutableListOf<String>()

        unique.forEach { uid ->
            val cached = nameCache[uid]
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
                    nameCache[doc.id] = name
                    result[doc.id] = name
                }
            } catch (_: Exception) {
                // Best-effort: missing names just don't appear in the result map.
            }
        }
        return result
    }
}
