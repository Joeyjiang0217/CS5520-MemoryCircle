package com.cs5520group15.memorycircle.ui.groupdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.AuthRepository
import com.cs5520group15.memorycircle.data.FirebaseModule
import com.cs5520group15.memorycircle.data.GroupRepository
import com.cs5520group15.memorycircle.model.Member
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * What: Holds the state shown on the group detail page — the group name, the
 *       flat member list (used both as thumbnails and as the "view all" roster),
 *       and the list of per-month scrapbook entries that belong to this group.
 *       All three are now live Firestore subscriptions:
 *         - groupName: groups/{gid}.name
 *         - members:   groups/{gid}/members subcollection
 *         - months:    groups/{gid}/scrapbooks subcollection
 * Who: Used by GroupDetailScreen.
 * When: Created when the screen is shown for a specific groupId; survives
 *       config changes. Listeners detached in onCleared().
 */
class GroupDetailViewModel : ViewModel() {

    /**
     * One scrapbook bucket for this group within a single month. `id` is the
     * Firestore doc id ("YYYY-MM"); `month`/`year` are display-formatted from it.
     */
    data class MonthScrapbook(
        val id:          String,
        val month:       String,   // "January"
        val year:        String,   // "2025"
        val memoryCount: Int,
        val colorType:   String    // "brown" or "sage"
    )

    private val _groupName = MutableStateFlow("")
    private val _members   = MutableStateFlow<List<Member>>(emptyList())
    private val _months    = MutableStateFlow<List<MonthScrapbook>>(emptyList())

    val groupName: StateFlow<String>              = _groupName.asStateFlow()
    val members:   StateFlow<List<Member>>        = _members.asStateFlow()
    val months:    StateFlow<List<MonthScrapbook>> = _months.asStateFlow()

    private var boundGroupId: String? = null

    private var groupListener:      ListenerRegistration? = null
    private var membersListener:    ListenerRegistration? = null
    private var scrapbooksListener: ListenerRegistration? = null

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

    /**
     * Subscribes to all three Firestore sources for `groupId`. Safe to call
     * multiple times — re-binds cleanly by detaching the old listeners first.
     */
    fun bind(groupId: String) {
        if (boundGroupId == groupId) return
        boundGroupId = groupId
        detachAll()

        val db        = FirebaseModule.db
        val groupRef  = db.collection("groups").document(groupId)
        val colorType = "brown"   // fallback color for scrapbook rows

        // 1) Group main doc — pull `name` (fallback to "Untitled" if missing).
        groupListener = groupRef.addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            _groupName.value = snap.getString("name") ?: "Untitled"
        }

        // 2) Members subcollection — ordered by joinedAt so the owner shows first.
        membersListener = groupRef.collection("members")
            .orderBy("joinedAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _members.value = snap.documents.map { doc ->
                    Member(
                        id             = doc.getString("uid") ?: doc.id,
                        name           = doc.getString("name") ?: "Member",
                        sharedMemories = 0,
                        isOnline       = false
                    )
                }
            }

        // 3) Scrapbooks subcollection — newest month first (doc id is "YYYY-MM"
        //    so descending document-name sort = newest first).
        scrapbooksListener = groupRef.collection("scrapbooks")
            .orderBy(com.google.firebase.firestore.FieldPath.documentId(), Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _months.value = snap.documents.mapNotNull { doc ->
                    val id = doc.id
                    val parsed = runCatching { YearMonth.parse(id) }.getOrNull() ?: return@mapNotNull null
                    MonthScrapbook(
                        id          = id,
                        month       = parsed.format(monthFormatter),
                        year        = parsed.year.toString(),
                        memoryCount = (doc.getLong("postCount") ?: 0L).toInt(),
                        colorType   = colorType
                    )
                }
            }
    }

    /**
     * Removes the current user from the bound group in Firestore, then calls
     * `onDone` so the screen can navigate back.
     */
    fun leaveGroup(onDone: () -> Unit) {
        val groupId = boundGroupId ?: return
        val uid = AuthRepository.currentUid ?: return
        viewModelScope.launch {
            runCatching { GroupRepository.leaveGroup(groupId, uid) }
                .onSuccess { onDone() }
        }
    }

    private fun detachAll() {
        groupListener?.remove();      groupListener = null
        membersListener?.remove();    membersListener = null
        scrapbooksListener?.remove(); scrapbooksListener = null
    }

    override fun onCleared() {
        super.onCleared()
        detachAll()
    }
}
