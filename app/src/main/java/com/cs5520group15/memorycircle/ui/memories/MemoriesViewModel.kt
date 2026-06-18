package com.cs5520group15.memorycircle.ui.memories

import androidx.lifecycle.ViewModel
import com.cs5520group15.memorycircle.data.AuthRepository
import com.cs5520group15.memorycircle.data.FirebaseModule
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * Who: Used by MemoriesScreen.
 * When: Created when MemoriesScreen is first displayed; survives config changes.
 */
class MemoriesViewModel : ViewModel() {

    /** One scrapbook belonging to a single group within a month. */
    data class Scrapbook(
        val id:          String,
        val groupId:     String,
        val groupName:   String,
        val memoryCount: Int,
        val colorType:   String
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

                // Detach listeners for groups the user is no longer in
                // (e.g. after leaveGroup).
                val toRemove = scrapbookListeners.keys - currentGroupIds
                toRemove.forEach { groupId ->
                    scrapbookListeners.remove(groupId)?.remove()
                    groupScrapbooks.remove(groupId)
                    groupMeta.remove(groupId)
                }

                rebuild()   // rebuild even if no scrapbooks changed (group name/color may have)
            }
    }

    private fun rebuild() {
        val byMonth = mutableMapOf<String, MutableList<Scrapbook>>()

        groupScrapbooks.forEach { (groupId, sbDocs) ->
            val (groupName, colorType) = groupMeta[groupId] ?: return@forEach
            sbDocs.forEach { sb ->
                val key = sb.id   // "YYYY-MM"
                runCatching { YearMonth.parse(key) }.getOrNull() ?: return@forEach
                byMonth.getOrPut(key) { mutableListOf() }.add(
                    Scrapbook(
                        id          = "${groupId}_$key",
                        groupId     = groupId,
                        groupName   = groupName,
                        memoryCount = (sb.getLong("postCount") ?: 0L).toInt(),
                        colorType   = colorType
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

    override fun onCleared() {
        super.onCleared()
        groupsListener?.remove()
        groupsListener = null
        scrapbookListeners.values.forEach { it.remove() }
        scrapbookListeners.clear()
        groupScrapbooks.clear()
        groupMeta.clear()
    }
}
