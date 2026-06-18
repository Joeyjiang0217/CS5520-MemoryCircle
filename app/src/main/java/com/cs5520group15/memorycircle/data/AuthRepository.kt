package com.cs5520group15.memorycircle.data

import com.google.firebase.auth.FirebaseUser
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
     * Reads the current user's display name from Firestore.
     * Firebase Auth doesn't store name natively, so we read from users/{uid}.
     */
    suspend fun getCurrentUserName(): Result<String> {
        return try {
            val uid = currentUid ?: return Result.Error("Not logged in")
            val doc = db.collection("users").document(uid).get().await()
            val name = doc.getString("name") ?: "User"
            Result.Success(name)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get user name")
        }
    }
}
