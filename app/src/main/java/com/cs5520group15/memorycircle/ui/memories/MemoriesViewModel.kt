package com.cs5520group15.memorycircle.ui.memories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.AuthRepository
import com.cs5520group15.memorycircle.data.FirebaseModule
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * What: Holds the UI state for the Memories (calendar) screen.
 *       Real-time subscriptions on two levels:
 *         - the user's group list (groups where memberIds array-contains uid)
 *         - each member group's scrapbooks subcollection
 *       Two-level listening is required because creating a new group writes
 *       the group doc first and the seed scrapbook a few RPCs later — a
 *       one-shot .get() on scrapbooks racing the group-doc listener returns
 *       empty for the brand-new group.
 *
 *       Each rebuild also resolves a thumbnail URL per scrapbook by querying
 *       the most recent post in that month and taking its first photo. Cached
 *       per (groupId, sbId, postCount) so it only re-fetches when a new post
 *       changes the latest-photo for that scrapbook.
 * Who: Used by MemoriesScreen.
 * When: Created when MemoriesScreen is first displayed; survives config changes.
 */
class MemoriesViewModel : ViewModel() {

    /** One scrapbook belonging to a single group within a month. */
    data class Scrapbook(
        val id:           String,
        val groupId:      String,
        val groupName:    String,
        val memoryCount:  Int,
        val colorType:    String,
        val thumbnailUrl: String = ""   // download URL of the latest post's first photo
    )

    /** One month bucket holding every group's scrapbook for that month. */
    data class MonthSection(
        val month:      String,
        val year:       String,
        val scrapbooks: List<Scrapbook>
    )

    private val _months = MutableStateFlow<List<MonthSection>>(emptyList())
    val months: StateFlow<List<MonthSection>> = _months.asStateFlow()

    private var groupsListener: ListenerRegistration? = null

    /** groupId -> active listener on that group's scrapbooks subcollection. */
    private val scrapbookListeners = mutableMapOf<String, ListenerRegistration>()

    /** groupId -> (name, colorType) — updated whenever the groups snapshot fires. */
    private val groupMeta = mutableMapOf<String, Pair<String, String>>()

    /** groupId -> latest scrapbook document snapshots seen by the scrapbook listener. */
    private val groupScrapbooks = mutableMapOf<String, List<DocumentSnapshot>>()

    /**
     * groupId+sbId -> (postCount, thumbnailUrl). Cache hit means the scrapbook
     * hasn't gained a new post since we last queried — safe to skip the Firestore
     * read. Cache miss / postCount mismatch triggers a fresh fetch.
     */
    private val thumbnailCache = mutableMapOf<String, Pair<Long, String>>()

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

    init { loadMonths() }

    private fun loadMonths() {
        val uid = AuthRepository.currentUid ?: return
        groupsListener = FirebaseModule.db.collection("groups")
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                val currentGroupIds = snap.documents.map { it.id }.toSet()

                // Update group metadata.
                snap.documents.forEach { doc ->
                    groupMeta[doc.id] = (doc.getString("name") ?: "Untitled") to
                                        (doc.getString("colorType") ?: "brown")
                }

                // Attach scrapbook listeners for any new groups.
                currentGroupIds.forEach { groupId ->
                    if (groupId !in scrapbookListeners) {
                        scrapbookListeners[groupId] = FirebaseModule.db
                            .collection("groups").document(groupId)
                            .collection("scrapbooks")
                            .addSnapshotListener { sbSnap, sbErr ->
                                if (sbErr != null || sbSnap == null) return@addSnapshotListener
                                groupScrapbooks[groupId] = sbSnap.documents
                                rebuild()
                            }
                    }
                }

                // Detach listeners for groups the user is no longer in.
                val toRemove = scrapbookListeners.keys - currentGroupIds
                toRemove.forEach { groupId ->
                    scrapbookListeners.remove(groupId)?.remove()
                    groupScrapbooks.remove(groupId)
                    groupMeta.remove(groupId)
                }

                rebuild()
            }
    }

    /**
     * Rebuilds the month buckets. Resolves thumbnails for any scrapbook whose
     * cached postCount differs from the current snapshot (or that has no
     * cache entry yet). Publishes a first pass synchronously from the cache
     * so the UI doesn't blank out, then publishes again once new thumbnails
     * have come back.
     */
    private fun rebuild() {
        publish()   // immediate publish using whatever thumbnails are already cached

        viewModelScope.launch {
            var thumbnailsChanged = false
            groupScrapbooks.forEach { (groupId, sbDocs) ->
                sbDocs.forEach { sb ->
                    val postCount = sb.getLong("postCount") ?: 0L
                    val cacheKey  = "${groupId}_${sb.id}"
                    val cached    = thumbnailCache[cacheKey]
                    if (cached == null || cached.first != postCount) {
                        val url = fetchLatestPostThumbnail(groupId, sb.id)
                        thumbnailCache[cacheKey] = postCount to url
                        if (url.isNotBlank() || cached?.second.isNullOrBlank().not()) {
                            thumbnailsChanged = true
                        }
                    }
                }
            }
            if (thumbnailsChanged) publish()
        }
    }

    private fun publish() {
        val byMonth = mutableMapOf<String, MutableList<Scrapbook>>()

        groupScrapbooks.forEach { (groupId, sbDocs) ->
            val (groupName, colorType) = groupMeta[groupId] ?: return@forEach
            sbDocs.forEach { sb ->
                val key = sb.id   // "YYYY-MM"
                runCatching { YearMonth.parse(key) }.getOrNull() ?: return@forEach
                val cacheKey = "${groupId}_$key"
                byMonth.getOrPut(key) { mutableListOf() }.add(
                    Scrapbook(
                        id           = cacheKey,
                        groupId      = groupId,
                        groupName    = groupName,
                        memoryCount  = (sb.getLong("postCount") ?: 0L).toInt(),
                        colorType    = colorType,
                        thumbnailUrl = thumbnailCache[cacheKey]?.second.orEmpty()
                    )
                )
            }
        }

        _months.value = byMonth.entries
            .sortedByDescending { it.key }
            .map { (key, list) ->
                val ym = YearMonth.parse(key)
                MonthSection(
                    month      = ym.format(monthFormatter),
                    year       = ym.year.toString(),
                    scrapbooks = list.sortedBy { it.groupName }
                )
            }
    }

    /**
     * Returns the download URL of the first photo of the most recent post in
     * the given scrapbook, or "" if the scrapbook has no posts / no photos.
     * Blanket-catches Firestore errors so a transient network failure just
     * leaves the thumbnail blank instead of crashing the rebuild.
     */
    private suspend fun fetchLatestPostThumbnail(groupId: String, sbId: String): String {
        return runCatching {
            val postSnap = FirebaseModule.db.collection("groups").document(groupId)
                .collection("scrapbooks").document(sbId)
                .collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get().await()
            val post = postSnap.documents.firstOrNull() ?: return@runCatching ""
            val photos = post.get("photos") as? List<*>
            val first = photos?.firstOrNull() as? Map<*, *>
            (first?.get("url") as? String).orEmpty()
        }.getOrDefault("")
    }

    override fun onCleared() {
        super.onCleared()
        groupsListener?.remove()
        groupsListener = null
        scrapbookListeners.values.forEach { it.remove() }
        scrapbookListeners.clear()
        groupScrapbooks.clear()
        groupMeta.clear()
        thumbnailCache.clear()
    }
}
