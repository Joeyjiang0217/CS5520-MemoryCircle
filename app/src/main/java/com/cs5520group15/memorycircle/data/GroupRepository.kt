package com.cs5520group15.memorycircle.data

import android.util.Log
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/**
 * Lightweight group-level Firestore helpers. Keeps the write-side operations
 * (leave, kick, delete) and the small bits of ownership-transfer logic in one
 * place so callers (GroupDetailViewModel, future moderation tools) don't have
 * to reinvent them.
 */
object GroupRepository {

    private val db      = FirebaseModule.db
    private val storage = FirebaseModule.storage

    /**
     * Smart "leave group". Behaviour depends on who's leaving:
     *
     * - **Non-owner** → just arrayRemove + decrement memberCount.
     * - **Owner, others remain** → ownership transfers to the first remaining
     *   member in `memberIds` (deterministic, no admin-picker UI needed), and
     *   the leaver is removed in the same update.
     * - **Owner, last member** → the whole group is cascade-deleted (see
     *   [deleteGroup]) so no orphan scrapbooks/posts/comments/photos remain.
     */
    suspend fun leaveGroup(groupId: String, uid: String) {
        val groupRef = db.collection("groups").document(groupId)
        val snap     = groupRef.get().await()

        @Suppress("UNCHECKED_CAST")
        val memberIds = (snap.get("memberIds") as? List<String>) ?: emptyList()
        val ownerId   = snap.getString("ownerId")
        val isOwner   = ownerId == uid

        when {
            // Owner is the only member → cascade-delete the group.
            isOwner && memberIds.size <= 1 -> {
                deleteGroup(groupId)
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
     * Renames the group. Open to any member by product decision — there's no
     * owner-only gate here. Blank names are treated as no-ops so a typo + save
     * doesn't wipe the existing name.
     */
    suspend fun renameGroup(groupId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        db.collection("groups").document(groupId)
            .update("name", trimmed).await()
    }

    /** Owner-only on the caller side. Removes a specific member from a group. */
    suspend fun kickMember(groupId: String, memberUid: String) {
        db.collection("groups").document(groupId).update(mapOf(
            "memberIds"   to FieldValue.arrayRemove(memberUid),
            "memberCount" to FieldValue.increment(-1)
        )).await()
    }

    /**
     * Cascade-deletes an entire group. The Firestore client SDK does NOT
     * cascade subcollection deletes, so we walk the tree ourselves:
     *
     *   1) Read every scrapbook, its posts, and each post's comments.
     *      Subcollection queries fan out in parallel so a group with N
     *      scrapbooks × M posts takes ~2 round-trips instead of N + N×M.
     *
     *   2) Collect every photo's Storage path from each post's `photos`
     *      array, then delete those files in parallel. Missing files are
     *      ignored — we don't want one orphan to block the whole delete.
     *
     *   3) Delete every Firestore doc in batches of ≤450 (Firestore's batch
     *      limit is 500). Order matters only at the very end: the group
     *      document itself goes last so a mid-flight failure leaves the
     *      group's deletion attempt observable rather than silently orphaning
     *      most of its data behind a now-missing parent.
     *
     * NOTE: Multi-batch deletes are NOT transactional. On partial failure
     *       some docs survive. For production-grade cleanup this should move
     *       to a Cloud Function triggered on group-doc delete — the SDK
     *       fundamentally can't make this atomic from the client.
     */
    suspend fun deleteGroup(groupId: String): Unit = coroutineScope {
        val groupRef = db.collection("groups").document(groupId)

        // 1) Walk the tree (parallelized).
        val scrapbookDocs = runCatching {
            groupRef.collection("scrapbooks").get().await().documents
        }.getOrDefault(emptyList())

        val postDocs = scrapbookDocs.map { sb ->
            async {
                runCatching { sb.reference.collection("posts").get().await().documents }
                    .getOrDefault(emptyList())
            }
        }.awaitAll().flatten()

        val commentDocs = postDocs.map { post ->
            async {
                runCatching { post.reference.collection("comments").get().await().documents }
                    .getOrDefault(emptyList())
            }
        }.awaitAll().flatten()

        // 2) Collect Storage paths from every post's photos array, delete in parallel.
        val storagePaths = postDocs.flatMap { post ->
            @Suppress("UNCHECKED_CAST")
            val photos = (post.get("photos") as? List<Map<String, Any?>>) ?: emptyList()
            photos.mapNotNull { it["storagePath"] as? String }.filter { it.isNotBlank() }
        }
        storagePaths.map { path ->
            async {
                runCatching { storage.reference.child(path).delete().await() }
                    .onFailure { Log.w("GroupRepo", "Storage delete failed for $path", it) }
            }
        }.awaitAll()

        // 3) Delete every Firestore doc. Children first, group doc last.
        val allRefs = commentDocs.map { it.reference } +
                      postDocs.map { it.reference } +
                      scrapbookDocs.map { it.reference } +
                      listOf(groupRef)

        allRefs.chunked(450).forEach { chunk ->
            db.runBatch { batch -> chunk.forEach { batch.delete(it) } }.await()
        }
    }
}
