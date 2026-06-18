package com.cs5520group15.memorycircle.data

import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

/**
 * Lightweight group-level Firestore helpers shared by GroupDetail and other
 * group-aware screens. The richer "load group + members + scrapbooks" reads
 * still live in the per-screen ViewModels for now; this file only owns the
 * write-side operations that needed real persistence.
 */
object GroupRepository {

    private val db = FirebaseModule.db

    /**
     * Removes the current user from a group:
     *   - arrayRemove uid from groups/{groupId}.memberIds
     *   - decrement groups/{groupId}.memberCount by 1
     *
     * The two field updates land in a single update call so memberIds and
     * memberCount can never drift out of sync. There is no separate members
     * subcollection — `memberIds` IS the source of truth for membership.
     */
    suspend fun leaveGroup(groupId: String, uid: String) {
        db.collection("groups").document(groupId).update(mapOf(
            "memberIds"   to FieldValue.arrayRemove(uid),
            "memberCount" to FieldValue.increment(-1)
        )).await()
    }
}
