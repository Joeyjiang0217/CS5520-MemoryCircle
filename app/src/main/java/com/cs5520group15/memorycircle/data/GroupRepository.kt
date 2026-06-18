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
     * Removes the current user from a group atomically:
     *   - arrayRemove uid from groups/{groupId}.memberIds
     *   - decrement groups/{groupId}.memberCount by 1
     *   - delete groups/{groupId}/members/{uid}
     *
     * The three writes are bundled in a single batch so a group never ends up
     * with a stale memberIds entry or an orphan members subdoc on failure.
     */
    suspend fun leaveGroup(groupId: String, uid: String) {
        val groupRef = db.collection("groups").document(groupId)
        db.runBatch { batch ->
            batch.update(groupRef, mapOf(
                "memberIds"   to FieldValue.arrayRemove(uid),
                "memberCount" to FieldValue.increment(-1)
            ))
            batch.delete(groupRef.collection("members").document(uid))
        }.await()
    }
}
