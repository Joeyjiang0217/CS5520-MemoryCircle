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
 *
 * Member-list storage shape:
 *   groups/{gid}.memberIds  — array of uid strings. Source of truth for "is X
 *                             in this group". Drives the array-contains query
 *                             every screen uses to enumerate a user's groups,
 *                             and the array-membership check rules will use
 *                             to gate reads/writes.
 *   groups/{gid}/members/{uid} — denormalized display card per member, holding
 *                                {uid, name, avatarUrl, joinedAt}. Lets the
 *                                group-detail / group-members screens render
 *                                with a single subcollection listener instead
 *                                of N per-uid users-doc listeners.
 *
 * The two shapes must stay in sync: every add/remove of a uid in `memberIds`
 * also creates/deletes the matching members subdoc — that's the responsibility
 * of every write path that touches membership (here, plus CreateGroupViewModel
 * for the create case).
 */
object GroupRepository {

    private val db      = FirebaseModule.db
    private val storage = FirebaseModule.storage

    /**
     * Lazy reconciliation: ensures groups/{gid}/members has a subdoc for every
     * uid currently in `memberIds`, and that every existing subdoc carries
     * the full {name, avatarUrl, bio} card. Missing uids get a fresh subdoc
     * including a server `joinedAt`; pre-existing subdocs missing `bio` (from
     * before bio was denormalized) are patched in place without disturbing
     * other fields.
     *
     * Used by GroupDetail / GroupMembers screens on bind. Idempotent — a
     * fully-populated subcollection is a no-op (no users-doc read, no writes).
     */
    suspend fun reconcileMembers(groupId: String) {
        val groupRef = db.collection("groups").document(groupId)
        val snap = runCatching { groupRef.get().await() }.getOrNull() ?: return
        @Suppress("UNCHECKED_CAST")
        val memberIds = (snap.get("memberIds") as? List<String>) ?: return
        if (memberIds.isEmpty()) return

        val existingDocs = runCatching { groupRef.collection("members").get().await() }
            .getOrNull()?.documents ?: return
        val existingById = existingDocs.associateBy { it.id }

        val toResolve = memberIds.filter { uid ->
            val doc = existingById[uid]
            doc == null || doc.getString("bio") == null
        }
        if (toResolve.isEmpty()) return

        val briefs = AuthRepository.getUserBriefs(toResolve)
        toResolve.forEach { uid ->
            val brief = briefs[uid]
            val ref   = groupRef.collection("members").document(uid)
            runCatching {
                if (existingById[uid] == null) {
                    ref.set(mapOf(
                        "uid"       to uid,
                        "name"      to (brief?.name.orEmpty()),
                        "avatarUrl" to (brief?.avatarUrl.orEmpty()),
                        "bio"       to (brief?.bio.orEmpty()),
                        "joinedAt"  to FieldValue.serverTimestamp()
                    )).await()
                } else {
                    // Only patch the missing fields, preserve joinedAt etc.
                    ref.update(mapOf(
                        "name"      to (brief?.name.orEmpty()),
                        "avatarUrl" to (brief?.avatarUrl.orEmpty()),
                        "bio"       to (brief?.bio.orEmpty())
                    )).await()
                }
            }
        }
    }

    /**
     * Smart "leave group". Behaviour depends on who's leaving:
     *
     * - **Non-owner** → arrayRemove + decrement memberCount + delete the
     *   member subdoc for the leaver.
     * - **Owner, others remain** → ownership transfers to the first remaining
     *   member in `memberIds` (deterministic, no admin-picker UI needed), the
     *   leaver is removed in the same update, and their member subdoc is
     *   deleted.
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
                runCatching { groupRef.collection("members").document(uid).delete().await() }
            }

            // Regular member leaving → just remove from membership.
            else -> {
                groupRef.update(mapOf(
                    "memberIds"   to FieldValue.arrayRemove(uid),
                    "memberCount" to FieldValue.increment(-1)
                )).await()
                runCatching { groupRef.collection("members").document(uid).delete().await() }
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

    /**
     * Adds the given uids to `groupId` as new members. For each uid the call:
     *   1) arrayUnion-s the uid into memberIds (idempotent — already-member
     *      uids are no-ops at the array level)
     *   2) increments memberCount by the count of uids that were actually new
     *      (we filter against the current snapshot first so the counter stays
     *      accurate even when the caller passes a uid that was already a member)
     *   3) writes groups/{gid}/members/{uid} with {uid, name, avatarUrl, bio,
     *      joinedAt} — same shape as CreateGroupViewModel uses on create.
     *
     * Returns the count of uids that were actually newly added.
     */
    suspend fun inviteMembers(groupId: String, uids: List<String>): Int {
        if (uids.isEmpty()) return 0
        val groupRef = db.collection("groups").document(groupId)
        val snap = groupRef.get().await()
        @Suppress("UNCHECKED_CAST")
        val current = (snap.get("memberIds") as? List<String>)?.toSet() ?: emptySet()
        val toAdd = uids.distinct().filterNot { it in current }
        if (toAdd.isEmpty()) return 0

        groupRef.update(mapOf(
            "memberIds"   to FieldValue.arrayUnion(*toAdd.toTypedArray()),
            "memberCount" to FieldValue.increment(toAdd.size.toLong())
        )).await()

        val briefs = AuthRepository.getUserBriefs(toAdd)
        toAdd.forEach { uid ->
            val brief = briefs[uid]
            runCatching {
                groupRef.collection("members").document(uid).set(mapOf(
                    "uid"       to uid,
                    "name"      to (brief?.name.orEmpty()),
                    "avatarUrl" to (brief?.avatarUrl.orEmpty()),
                    "bio"       to (brief?.bio.orEmpty()),
                    "joinedAt"  to FieldValue.serverTimestamp()
                )).await()
            }
        }
        return toAdd.size
    }

    /** Owner-only on the caller side. Removes a specific member from a group
     *  AND deletes their members/{uid} subdoc so the group screens stop
     *  rendering the kicked user. */
    suspend fun kickMember(groupId: String, memberUid: String) {
        val groupRef = db.collection("groups").document(groupId)
        groupRef.update(mapOf(
            "memberIds"   to FieldValue.arrayRemove(memberUid),
            "memberCount" to FieldValue.increment(-1)
        )).await()
        runCatching { groupRef.collection("members").document(memberUid).delete().await() }
    }

    /**
     * Cascade-deletes an entire group. The Firestore client SDK does NOT
     * cascade subcollection deletes, so we walk the tree ourselves:
     *
     *   1) Read every scrapbook, its posts, each post's comments, AND the
     *      members subcollection. Subcollection queries fan out in parallel so
     *      a group with N scrapbooks × M posts takes ~2 round-trips instead of
     *      N + N×M.
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
        val scrapbookDocsAsync = async {
            runCatching { groupRef.collection("scrapbooks").get().await().documents }
                .getOrDefault(emptyList())
        }
        val memberDocsAsync = async {
            runCatching { groupRef.collection("members").get().await().documents }
                .getOrDefault(emptyList())
        }

        val scrapbookDocs = scrapbookDocsAsync.await()
        val memberDocs    = memberDocsAsync.await()

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
                      memberDocs.map { it.reference } +
                      listOf(groupRef)

        allRefs.chunked(450).forEach { chunk ->
            db.runBatch { batch -> chunk.forEach { batch.delete(it) } }.await()
        }
    }
}
