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
     * matching users/{uid} document.
     *
     * The full email is held only by Firebase Auth — the public users doc gets
     * `emailMasked` (e.g. "j***@example.com") so any signed-in user can render
     * a friend / member row without exposing addresses to the broader signed-in
     * audience. The signed-in user reads their own real email via
     * FirebaseAuth.currentUser.email.
     */
    suspend fun register(name: String, email: String, password: String): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user!!.uid

            val userDoc = mapOf(
                "uid"         to uid,
                "name"        to name,
                "emailMasked" to maskEmail(email),
                "bio"         to "",
                "avatarUrl"   to "",
                "createdAt"   to FieldValue.serverTimestamp()
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

    // ── Profile lookup ───────────────────────────────────────────────────────

    /**
     * What: A user's render-time identity — display name and avatar download
     *       URL — surfaced together so callers (scrapbook author rows, group
     *       creation flows, etc.) get both fields in one Firestore round trip
     *       instead of querying for them separately.
     */
    data class UserBrief(val name: String, val avatarUrl: String, val bio: String = "")

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
     * Single-uid name lookup via a one-shot users/{uid} read. Surfaces wherever
     * only the name is needed (HomeScreen greeting, etc.).
     */
    suspend fun getUserName(uid: String): String? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            doc.getString("name")
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
     * What: One-shot batched (uid → name + avatarUrl) lookup via whereIn on
     *       documentId. Returns a map covering every input id whose users doc
     *       exists; missing ids are absent. Chunked at 30 to respect Firestore's
     *       whereIn limit.
     * Who:  Used by ScrapbookRepository.assemble() to attach author identity
     *       to post / photo / comment rows at read time.
     * When: One call per assemble pass. No caching — assemble happens inside a
     *       snapshot listener so each fire reads fresh values; for surfaces
     *       that need live name/avatar updates (Friends list, group members)
     *       the consumer attaches its own snapshot listener instead.
     */
    suspend fun getUserBriefs(uids: List<String>): Map<String, UserBrief> {
        val unique = uids.distinct().filter { it.isNotBlank() }
        if (unique.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, UserBrief>()
        unique.chunked(30).forEach { chunk ->
            try {
                val snap = db.collection("users")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get().await()
                snap.documents.forEach { doc ->
                    val name = doc.getString("name") ?: return@forEach
                    result[doc.id] = UserBrief(
                        name      = name,
                        avatarUrl = doc.getString("avatarUrl").orEmpty(),
                        bio       = doc.getString("bio").orEmpty()
                    )
                }
            } catch (_: Exception) {
                // Best-effort: missing entries simply don't appear in the map.
            }
        }
        return result
    }

    /**
     * What: Privacy mask for emails written into the public users doc as
     *       `emailMasked`. Keeps the first character of the local part, drops
     *       three stars after it, preserves @ + domain verbatim. Strings
     *       without an @ are returned unchanged — defensive against malformed
     *       input.
     *
     *       "1@test.com"          → "1***@test.com"
     *       "john.doe@gmail.com"  → "j***@gmail.com"
     */
    fun maskEmail(email: String): String {
        val at = email.indexOf('@')
        if (at <= 0) return email
        val local  = email.substring(0, at)
        val domain = email.substring(at)
        return "${local.first()}***$domain"
    }
}
