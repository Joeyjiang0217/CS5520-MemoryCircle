package com.cs5520group15.memorycircle.data

import android.net.Uri
import com.cs5520group15.memorycircle.model.Comment
import com.cs5520group15.memorycircle.model.Photo
import com.cs5520group15.memorycircle.model.ScrapbookEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

/**
 * Firestore-backed store for every group's scrapbook timeline. A real-time
 * snapshot listener keeps each group's StateFlow in sync.
 *
 * Firestore layout (one scrapbook per group per month, id = "YYYY-MM"):
 *   groups/{groupId}/scrapbooks/{scrapbookId}/posts/{postId}
 *     postId, authorId, title, date (Timestamp), tags (List<String>),
 *     photos (List<Map> of { photoId, url, storagePath, description,
 *     uploaderId, uploadedAt }), commentCount (Int), createdAt (Timestamp)
 *   groups/{groupId}/scrapbooks/{scrapbookId}/posts/{postId}/comments/{commentId}
 *     commentId, authorId, text, createdAt (Timestamp)
 *
 * Author display names are NOT persisted on posts or comments — only the
 * uid is stored. assemble() looks each unique authorId up via
 * AuthRepository.getUserNames() (batched + cached) so renaming a user
 * propagates without any fan-out across thousands of historical docs.
 */
object ScrapbookRepository {

    private val db = FirebaseModule.db

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val flows     = mutableMapOf<String, MutableStateFlow<List<ScrapbookEntry>>>()
    private val listeners = mutableMapOf<String, ListenerRegistration>()

    private val dayLabel = SimpleDateFormat("MMMM d", Locale.ENGLISH)

    /** The current month's scrapbook id, e.g. "2026-06". */
    private fun currentScrapbookId(): String = YearMonth.now().toString()

    private fun postsRef(groupId: String, scrapbookId: String): CollectionReference =
        db.collection("groups").document(groupId)
            .collection("scrapbooks").document(scrapbookId)
            .collection("posts")

    /**
     * Returns the live timeline flow for a group, attaching a Firestore
     * snapshot listener on the current month's posts on first use.
     */
    fun entriesFor(groupId: String): StateFlow<List<ScrapbookEntry>> = flow(groupId).asStateFlow()

    private fun flow(groupId: String): MutableStateFlow<List<ScrapbookEntry>> {
        flows[groupId]?.let { return it }
        val f = MutableStateFlow<List<ScrapbookEntry>>(emptyList())
        flows[groupId] = f
        startListening(groupId, currentScrapbookId())
        return f
    }

    private fun startListening(groupId: String, scrapbookId: String) {
        if (listeners.containsKey(groupId)) return
        val registration = postsRef(groupId, scrapbookId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val docs = snapshot.documents
                scope.launch {
                    try {
                        flows[groupId]?.value = assemble(docs)
                    } catch (_: Exception) {
                        // Leave the current list untouched on assembly failure.
                    }
                }
            }
        listeners[groupId] = registration
    }

    /**
     * Builds the ScrapbookEntry list for a set of post documents. Fetches each
     * post's comments subcollection AND batch-resolves every unique authorId
     * (across posts + comments) into a display name via AuthRepository.
     */
    private suspend fun assemble(postDocs: List<DocumentSnapshot>): List<ScrapbookEntry> {
        // First pass: load comments for every post, collect every unique authorId.
        data class Raw(
            val doc: DocumentSnapshot,
            val photos: List<Photo>,
            val rawComments: List<Pair<String /*commentId*/, Map<String, Any?>>>,
            val commentAuthorIds: List<String>
        )

        val raws = postDocs.map { doc ->
            val photos = (doc.get("photos") as? List<*>).orEmpty()
                .filterIsInstance<Map<*, *>>()
                .map { m ->
                    Photo(
                        photoId     = m["photoId"] as? String ?: "",
                        url         = m["url"] as? String ?: "",
                        storagePath = m["storagePath"] as? String ?: "",
                        description = m["description"] as? String ?: "",
                        uploaderId  = m["uploaderId"] as? String ?: ""
                    )
                }

            val commentsSnap = doc.reference.collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get().await()
            val rawComments = commentsSnap.documents.map { cm ->
                cm.id to mapOf<String, Any?>(
                    "authorId" to (cm.getString("authorId") ?: ""),
                    "text"     to (cm.getString("text") ?: "")
                )
            }
            val commentAuthorIds = rawComments.mapNotNull { (_, m) -> m["authorId"] as? String }
                .filter { it.isNotBlank() }

            Raw(doc, photos, rawComments, commentAuthorIds)
        }

        val allAuthorIds = (
            raws.mapNotNull { it.doc.getString("authorId") } +
            raws.flatMap { it.commentAuthorIds }
        ).filter { it.isNotBlank() }.distinct()

        val nameMap = AuthRepository.getUserNames(allAuthorIds)

        // Second pass: assemble final ScrapbookEntry with names filled in.
        return raws.map { (doc, photos, rawComments, _) ->
            val authorId   = doc.getString("authorId") ?: ""
            val authorName = nameMap[authorId] ?: ""

            val comments = rawComments.map { (commentId, m) ->
                val cAuthorId = m["authorId"] as? String ?: ""
                Comment(
                    id         = commentId,
                    authorId   = cAuthorId,
                    authorName = nameMap[cAuthorId] ?: "",
                    text       = m["text"] as? String ?: ""
                )
            }

            val dateLabel = doc.getTimestamp("date")?.toDate()?.let { dayLabel.format(it) } ?: ""

            ScrapbookEntry(
                id           = doc.id,
                authorId     = authorId,
                authorName   = authorName,
                date         = dateLabel,
                title        = doc.getString("title") ?: "",
                tags         = (doc.get("tags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                photos       = photos,
                comments     = comments,
                commentCount = (doc.getLong("commentCount") ?: 0L).toInt()
            )
        }
    }

    private suspend fun refreshGroup(groupId: String) {
        val snap = postsRef(groupId, currentScrapbookId())
            .orderBy("date", Query.Direction.DESCENDING)
            .get().await()
        flows[groupId]?.value = assemble(snap.documents)
    }

    /**
     * Adds a memory post. With `joinPostId == null` this creates a brand-new
     * post (title + tags + first photo). With `joinPostId` set it appends
     * this member's photo to that existing post's photos field.
     */
    suspend fun addPost(
        groupId:          String,
        title:            String,
        tags:             List<String>,
        description:      String,
        selectedPhotoUri: String,
        date:             LocalDate,
        joinPostId:       String? = null
    ) {
        val scrapbookId = currentScrapbookId()
        val uid = AuthRepository.currentUid ?: ""

        if (joinPostId == null) {
            val dateTimestamp = Timestamp(
                date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(),
                0
            )
            val postRef = postsRef(groupId, scrapbookId).document()
            val postId  = postRef.id
            val (url, storagePath) = uploadPhoto(selectedPhotoUri, groupId, postId)
            val photo = photoMap(url, storagePath, description, uid)

            postRef.set(mapOf(
                "postId"       to postId,
                "authorId"     to uid,            // ← display name NOT stored
                "title"        to title,
                "date"         to dateTimestamp,
                "tags"         to tags,
                "photos"       to listOf(photo),
                "commentCount" to 0,
                "createdAt"    to FieldValue.serverTimestamp()
            )).await()

            db.collection("groups").document(groupId)
                .collection("scrapbooks").document(scrapbookId)
                .update("postCount", FieldValue.increment(1)).await()

            db.collection("groups").document(groupId)
                .update("memoryCount", FieldValue.increment(1)).await()
        } else {
            val postRef = postsRef(groupId, scrapbookId).document(joinPostId)
            val (url, storagePath) = uploadPhoto(selectedPhotoUri, groupId, joinPostId)
            val photo = photoMap(url, storagePath, description, uid)
            postRef.update("photos", FieldValue.arrayUnion(photo)).await()
        }
        refreshGroup(groupId)
    }

    fun entry(groupId: String, entryId: String): ScrapbookEntry? =
        flows[groupId]?.value?.firstOrNull { it.id == entryId }

    suspend fun updateTitle(groupId: String, entryId: String, title: String) {
        val newTitle = title.ifBlank { return }
        postsRef(groupId, currentScrapbookId()).document(entryId)
            .update("title", newTitle).await()
    }

    suspend fun updatePhotoDescription(
        groupId: String,
        entryId: String,
        photoId: String,
        description: String
    ) {
        val postRef = postsRef(groupId, currentScrapbookId()).document(entryId)
        val snapshot = postRef.get().await()
        @Suppress("UNCHECKED_CAST")
        val photos = (snapshot.get("photos") as? List<Map<String, Any?>>).orEmpty()
        val updated = photos.map { photo ->
            if (photo["photoId"] == photoId) {
                photo.toMutableMap().apply { this["description"] = description }
            } else photo
        }
        postRef.update("photos", updated).await()
    }

    /**
     * Appends a member's comment. Only the authorId is stored — the display
     * name is resolved at read time. Blank text is ignored.
     */
    suspend fun addComment(groupId: String, entryId: String, text: String) {
        if (text.isBlank()) return
        val uid = AuthRepository.currentUid ?: return
        val postRef = postsRef(groupId, currentScrapbookId()).document(entryId)
        val commentRef = postRef.collection("comments").document()
        commentRef.set(mapOf(
            "commentId" to commentRef.id,
            "authorId"  to uid,
            "text"      to text.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
        postRef.update("commentCount", FieldValue.increment(1)).await()
        refreshGroup(groupId)
    }

    fun detach(groupId: String) {
        listeners.remove(groupId)?.remove()
        flows.remove(groupId)
    }

    fun detachAll() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
        flows.clear()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private suspend fun uploadPhoto(localUri: String, groupId: String, postId: String): Pair<String, String> {
        val photoId = UUID.randomUUID().toString()
        val path = "groups/$groupId/scrapbooks/${currentScrapbookId()}/posts/$postId/$photoId.jpg"
        val ref = FirebaseModule.storage.reference.child(path)
        ref.putFile(Uri.parse(localUri)).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        return downloadUrl to path
    }

    private fun photoMap(url: String, storagePath: String, description: String, uploaderId: String): Map<String, Any> =
        mapOf(
            "photoId"     to storagePath.substringAfterLast('/').removeSuffix(".jpg"),
            "url"         to url,
            "storagePath" to storagePath,
            "description" to description,
            "uploaderId"  to uploaderId,
            "uploadedAt"  to Timestamp.now()
        )
}
