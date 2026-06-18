package com.cs5520group15.memorycircle.data

import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

/**
 * Lightweight group-level Firestore helpers. Keeps the write-side operations
 * (leave, kick, delete) and the small bits of ownership-transfer logic in one
 * place so callers (GroupDetailViewModel, future moderation tools) don't have
 * to reinvent them.
 */
object GroupRepository {

    private val db = FirebaseModule.db

    /**
     * Smart "leave group". Behaviour depends on who's leaving:
     *
     * - **Non-owner** → just arrayRemove + decrement memberCount.
     * - **Owner, others remain** → ownership transfers to the first remaining
     *   member in `memberIds` (deterministic, no admin-picker UI needed), and
     *   the leaver is removed in the same batch.
     * - **Owner, last member** → the group document is deleted entirely.
     *
     * NOTE: When a group is deleted here, its scrapbooks/posts/comments
     *       subcollections + Storage photos become orphans. Cleaning those up
     *       cleanly requires a Cloud Function (Firestore can't cascade-delete
     *       subcollections client-side). For the course project, leaving them
     *       behind is acceptable — they're just unreachable bytes.
     */
    suspend fun leaveGroup(groupId: String, uid: String) {
        val groupRef = db.collection("groups").document(groupId)
        val snap     = groupRef.get().await()

        @Suppress("UNCHECKED_CAST")
        val memberIds = (snap.get("memberIds") as? List<String>) ?: emptyList()
        val ownerId   = snap.getString("ownerId")
        val isOwner   = ownerId == uid

        when {
            // Owner is the only member → delete the whole group.
            isOwner && memberIds.size <= 1 -> {
                groupRef.delete().await()
            }

            // Owner is leaving but others remain → transfer ownership.
            isOwner -> {
                val newOwner = memberIds.firstOrNull { it != uid } ?: return
                groupRef.update(mapOf(
                    "ownerId"     to newOwner,
                    "memberIds"   to FieldValue.arrayRemove(uid),
                    "memberCount" to FieldValue.increment(-1)
                )).await()
            }

            // Regular member leaving → just remove from membership.
            else -> {
                groupRef.update(mapOf(
                    "memberIds"   to FieldValue.arrayRemove(uid),
                    "memberCount" to FieldValue.increment(-1)
                )).await()
            }
        }
    }

    /**
     * Removes a specific member from a group. Owner-only on the caller side
     * (UI guards this). If somehow called by a non-owner, Firestore would
     * accept it in dev — security rules will gate it in production.
     */
    suspend fun kickMember(groupId: String, memberUid: String) {
        db.collection("groups").document(groupId).update(mapOf(
            "memberIds"   to FieldValue.arrayRemove(memberUid),
            "memberCount" to FieldValue.increment(-1)
        )).await()
    }

    /**
     * Deletes the entire group document. Subcollections (scrapbooks, comments)
     * and Storage photos are left as orphans — see leaveGroup() note.
     * Owner-only on the caller side.
     */
    suspend fun deleteGroup(groupId: String) {
        db.collection("groups").document(groupId).delete().await()
    }
}
