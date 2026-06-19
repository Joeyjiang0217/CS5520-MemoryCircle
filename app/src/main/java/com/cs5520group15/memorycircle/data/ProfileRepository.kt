package com.cs5520group15.memorycircle.data

import android.net.Uri
import com.cs5520group15.memorycircle.model.Profile
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * What: Backs the Profile / EditProfile / AvatarViewer screens with a live
 *       Firestore subscription on users/{uid}. The same StateFlow is shared by
 *       all three screens, so a save on EditProfile is visible on Profile the
 *       moment Firestore acknowledges the write (no manual refresh).
 *
 *       email fallback: if the users doc's email field is blank, we surface
 *       FirebaseAuth.currentUser.email instead so the row never reads as
 *       empty for newly-registered accounts whose doc only had `name` set.
 *
 *       Avatar upload mirrors ScrapbookRepository.uploadPhoto — pushes the
 *       picked Uri to Storage at avatars/{uid}.jpg and patches the doc's
 *       avatarUrl with the resulting download URL.
 *
 * Who: Used by ProfileViewModel and EditProfileViewModel.
 * When: bind() is called from every viewmodel's init block; the listener
 *       survives until the user logs out and a different uid binds.
 */
object ProfileRepository {

    private val db      = FirebaseModule.db
    private val storage = FirebaseModule.storage
    private val auth    = FirebaseModule.auth

    private val _profile = MutableStateFlow(Profile("", "", "", ""))
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    private var listener: ListenerRegistration? = null
    private var boundUid: String? = null

    /**
     * Attaches the Firestore snapshot listener for the currently signed-in
     * user. Idempotent for the same uid; rebinds cleanly when a different
     * user signs in (e.g. after logout + re-login).
     */
    fun bind() {
        val uid = AuthRepository.currentUid
        if (uid == boundUid && listener != null) return
        detach()
        boundUid = uid
        if (uid == null) {
            _profile.value = Profile("", "", "", "")
            return
        }
        val authEmail = auth.currentUser?.email.orEmpty()
        listener = db.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val docEmail = snap.getString("email").orEmpty()
                _profile.value = Profile(
                    name      = snap.getString("name").orEmpty(),
                    bio       = snap.getString("bio").orEmpty(),
                    email     = docEmail.ifBlank { authEmail },
                    avatarUrl = snap.getString("avatarUrl").orEmpty()
                )
            }
    }

    fun detach() {
        listener?.remove()
        listener = null
        boundUid = null
    }

    /** Writes the new display name; blank input is silently ignored. */
    suspend fun updateName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val uid = AuthRepository.currentUid ?: return
        db.collection("users").document(uid)
            .update("name", trimmed).await()
    }

    /** Bio may be blanked out intentionally — trimmed but not rejected when empty. */
    suspend fun updateBio(bio: String) {
        val uid = AuthRepository.currentUid ?: return
        db.collection("users").document(uid)
            .update("bio", bio.trim()).await()
    }

    /**
     * Updates the user's display email on the users doc. NOTE: this does NOT
     * change the Firebase Auth email used to sign in — that requires a
     * recent-credential re-auth and lives in a future flow.
     */
    suspend fun updateEmail(email: String) {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return
        val uid = AuthRepository.currentUid ?: return
        db.collection("users").document(uid)
            .update("email", trimmed).await()
    }

    /**
     * Uploads the picked local Uri to avatars/{uid}.jpg and writes the
     * resulting download URL to the user's doc. Returns the new URL.
     * Throws on auth-not-signed-in or any Storage/Firestore failure so the
     * caller can surface a snackbar.
     */
    suspend fun uploadAvatar(localUri: String): String {
        val uid = AuthRepository.currentUid ?: error("Not logged in")
        val path = "avatars/$uid.jpg"
        val ref  = storage.reference.child(path)
        ref.putFile(Uri.parse(localUri)).await()
        val url = ref.downloadUrl.await().toString()
        db.collection("users").document(uid)
            .update("avatarUrl", url).await()
        return url
    }
}
