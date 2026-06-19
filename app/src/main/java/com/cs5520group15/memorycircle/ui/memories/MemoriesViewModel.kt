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

class MemoriesViewModel : ViewModel() {

    data class Scrapbook(
        val id:           String,
        val groupId:      String,
        val groupName:    String,
        val memoryCount:  Int,
        val colorType:    String,
        val thumbnailUrl: String = ""
    )

    data class MonthSection(
        val month:      String,
        val year:       String,
        val scrapbooks: List<Scrapbook>
    )

    private val _months = MutableStateFlow<List<MonthSection>>(emptyList())
    val months: StateFlow<List<MonthSection>> = _months.asStateFlow()

    private var groupsListener: ListenerRegistration? = null
    private val scrapbookListeners = mutableMapOf<String, ListenerRegistration>()
    private val groupMeta = mutableMapOf<String, Pair<String, String>>()
    private val groupScrapbooks = mutableMapOf<String, List<DocumentSnapshot>>()
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

                snap.documents.forEach { doc ->
                    groupMeta[doc.id] = (doc.getString("name") ?: "Untitled") to
                            (doc.getString("colorType") ?: "brown")
                }

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

                val toRemove = scrapbookListeners.keys - currentGroupIds
                toRemove.forEach { groupId ->
                    scrapbookListeners.remove(groupId)?.remove()
                    groupScrapbooks.remove(groupId)
                    groupMeta.remove(groupId)
                }

                rebuild()
            }
    }

    private fun rebuild() {
        publish()

        viewModelScope.launch {
            var thumbnailsChanged = false
            val snapshot = groupScrapbooks.toMap()  // snapshot to avoid ConcurrentModificationException
            snapshot.forEach { (groupId, sbDocs) ->
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
                val key = sb.id
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