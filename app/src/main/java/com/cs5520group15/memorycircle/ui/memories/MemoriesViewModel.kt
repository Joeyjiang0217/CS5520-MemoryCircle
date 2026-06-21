/**
 * What: ViewModel that holds UI state and business logic for the Memories screen.
 * Who:  Used by MemoriesScreen.
 * When: Created when MemoriesScreen is first composed; survives configuration changes.
 */

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

    /**
     * What: Called by MemoriesScreen's LaunchedEffect on every entry. Always
     *       tears down the existing listener tree and re-attaches fresh
     *       listeners for the current signed-in uid. This is intentionally
     *       unconditional because a non-null `groupsListener` does NOT mean
     *       the underlying Firestore subscription is still alive.
     *
     *       Concrete failure mode we ran into:
     *         1. User deletes a group from GroupDetail.
     *         2. GroupRepository.deleteGroup is a cascading delete — children
     *            (scrapbooks, posts, members) are batched first, then the
     *            group doc itself.
     *         3. The scrapbookListener watching `groups/{oldId}/scrapbooks`
     *            re-runs the security rule each event; once the group doc is
     *            gone, `groupDoc(gid).memberIds` fails the rule, the server
     *            detaches the subscription and sends a single error event,
     *            and we silently swallow it via `if (err != null) return`.
     *            The ListenerRegistration object is still non-null but the
     *            subscription is dead. The shared gRPC connection used by the
     *            sibling groupsListener takes the hit too — new groups don't
     *            propagate until the next app start.
     *         4. User creates a new group + seeds: Firestore writes succeed,
     *            GroupDetail (which spins up a *fresh* listener on entry)
     *            shows them, but Memories' dead listeners stay silent.
     *
     *       Re-binding on every screen entry costs nothing visible (Firestore
     *       client cache hydrates the new listener instantly) and guarantees
     *       we recover from this and any similar listener-died state.
     * Who: Called from MemoriesScreen's LaunchedEffect(Unit) on every entry.
     * When: Each time the screen enters composition.
     */
    fun bind() {
        detachAll()
        loadMonths()
    }

    private fun detachAll() {
        groupsListener?.remove()
        groupsListener = null
        scrapbookListeners.values.forEach { it.remove() }
        scrapbookListeners.clear()
        groupScrapbooks.clear()
        groupMeta.clear()
    }

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
        detachAll()
        thumbnailCache.clear()
    }
}