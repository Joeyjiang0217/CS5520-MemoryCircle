package com.cs5520group15.memorycircle.data

import android.net.Uri
import com.cs5520group15.memorycircle.model.Profile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * What: Backs the Profile / EditProfile / AvatarViewer screens with a live
 *       Firestore subscription on users/{uid}. The same StateFlow is shared by
 *       all three screens, so a save on EditProfile is visible on Profile the
 *       moment Firestore acknowledges the write (no manual refresh).
 *
 *       The doc holds `emailMasked` for the public audience; the signed-in
 *       user sees their real email here via FirebaseAuth.currentUser.email
 *       (never persisted to Firestore).
 *
 *       Avatar upload mirrors ScrapbookRepository.uploadPhoto — pushes the
 *       picked Uri to Storage at avatars/{uid}.jpg and patches the doc's
 *       avatarUrl with the resulting download URL.
 *
 *       Denormalization fan-out: every successful write to name / avatarUrl
 *       (the two fields rendered on other people's screens) is mirrored into
 *       groups/{gid}/members/{me} for every group the user belongs to, so the
 *       group-detail / group-members surfaces see the change cross-device
 *       without needing a per-uid users listener of their own.
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var listener: ListenerRegistration? = null
    private var boundUid: String? = null
    private var maskedBackfillDone = false

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
        maskedBackfillDone = false
        if (uid == null) {
            _profile.value = Profile("", "", "", "")
            return
        }
        val authEmail = auth.currentUser?.email.orEmpty()
        listener = db.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _profile.value = Profile(
                    name      = snap.getString("name").orEmpty(),
                    bio       = snap.getString("bio").orEmpty(),
                    email     = authEmail,
                    avatarUrl = snap.getString("avatarUrl").orEmpty()
                )

                // One-shot self-heal: accounts registered before the
                // emailMasked split don't have the field populated. As soon as
                // we observe that, write the masked address derived from Auth
                // so the user's MemberProfile row shows the correct chip
                // without a manual Firestore-console edit. Idempotent via
                // [maskedBackfillDone].
                if (!maskedBackfillDone && snap.getString("emailMasked").isNullOrBlank()
                    && authEmail.isNotBlank()) {
                    maskedBackfillDone = true
                    scope.launch {
                        runCatching {
                            db.collection("users").document(uid)
                                .update(mapOf(
                                    "emailMasked" to AuthRepository.maskEmail(authEmail),
                                    "email"       to FieldValue.delete()
                                )).await()
                        }
                    }
                }
            }
    }

    fun detach() {
        listener?.remove()
        listener = null
        boundUid = null
    }

    /** Writes the new display name; blank input is silently ignored.
     *  Fans the new name out to every groups/{gid}/members/{me} subdoc so
     *  group screens reflect the rename without restart. */
    suspend fun updateName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val uid = AuthRepository.currentUid ?: return
        db.collection("users").document(uid)
            .update("name", trimmed).await()
        fanOutToMyGroupMembers(uid, mapOf("name" to trimmed))
    }

    /** Bio may be blanked out intentionally — trimmed but not rejected when empty.
     *  Fanned out into group members so the subtitle line under each row
     *  reflects edits cross-device without a re-bind. */
    suspend fun updateBio(bio: String) {
        val uid = AuthRepository.currentUid ?: return
        val trimmed = bio.trim()
        db.collection("users").document(uid)
            .update("bio", trimmed).await()
        fanOutToMyGroupMembers(uid, mapOf("bio" to trimmed))
    }

    /**
     * Updates only the displayable `emailMasked` field on the users doc.
     *
     * NOTE: this does NOT change the Firebase Auth email used to sign in —
     * Auth email changes require a recent-credential re-auth flow that lives
     * elsewhere. The masked value here is what other users see; the real
     * email continues to be sourced from FirebaseAuth.currentUser.email for
     * the signed-in user's own screens.
     */
    suspend fun updateEmail(email: String) {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return
        val uid = AuthRepository.currentUid ?: return
        db.collection("users").document(uid)
            .update("emailMasked", AuthRepository.maskEmail(trimmed)).await()
    }

    /**
     * Uploads the picked local Uri to avatars/{uid}.jpg and writes the
     * resulting download URL to the user's doc, then fans the new URL out to
     * every group the user belongs to so other devices see the change via
     * their group-members listener. Returns the new URL.
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
        fanOutToMyGroupMembers(uid, mapOf("avatarUrl" to url))
        return url
    }

    /**
     * Writes the supplied `updates` into groups/{gid}/members/{uid} for every
     * group the user belongs to. Failures on individual subdocs are swallowed
     * (best-effort) so a single rule rejection doesn't surface as a profile
     * save failure — the user's primary write (the users doc) already
     * succeeded by the time we get here.
     *
     * The membership query uses the `memberIds` array on the group doc, which
     * is the same source HomeViewModel uses to list a user's groups.
     */
    private suspend fun fanOutToMyGroupMembers(uid: String, updates: Map<String, Any>) {
        if (updates.isEmpty()) return
        try {
            val snap = db.collection("groups")
                .whereArrayContains("memberIds", uid)
                .get().await()
            snap.documents.forEach { doc ->
                runCatching {
                    db.collection("groups").document(doc.id)
                        .collection("members").document(uid)
                        .set(updates + mapOf("uid" to uid, "updatedAt" to FieldValue.serverTimestamp()),
                             com.google.firebase.firestore.SetOptions.merge())
                        .await()
                }
            }
        } catch (_: Exception) {
            // Best-effort fan-out; the primary users-doc write already succeeded.
        }
    }
}
