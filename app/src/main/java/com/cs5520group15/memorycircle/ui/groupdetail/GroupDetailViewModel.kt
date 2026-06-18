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
 *
 *       Two live Firestore subscriptions:
 *         - groups/{gid}        → name + memberIds (members list is derived
 *                                 from the array; the uids are batch-resolved
 *                                 to display names via AuthRepository).
 *         - groups/{gid}/scrapbooks → per-month timeline list.
 *
 *       There is no separate members subcollection — `memberIds` IS the
 *       membership source of truth and member display names are looked up at
 *       read time, same convention as posts/comments.
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
    private val _isOwner   = MutableStateFlow(false)

    val groupName: StateFlow<String>              = _groupName.asStateFlow()
    val members:   StateFlow<List<Member>>        = _members.asStateFlow()
    val months:    StateFlow<List<MonthScrapbook>> = _months.asStateFlow()
    /** True when the current logged-in user is the group's owner. Used by the
     *  screen to show owner-only affordances (kick member, delete group). */
    val isOwner:   StateFlow<Boolean>             = _isOwner.asStateFlow()

    private var boundGroupId: String? = null

    private var groupListener:      ListenerRegistration? = null
    private var scrapbooksListener: ListenerRegistration? = null

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

    fun bind(groupId: String) {
        if (boundGroupId == groupId) return
        boundGroupId = groupId
        detachAll()

        val db        = FirebaseModule.db
        val groupRef  = db.collection("groups").document(groupId)
        val colorType = "brown"   // fallback color for scrapbook rows

        // 1) Group main doc — pull `name`, derive members from memberIds, and
        //    flag whether the current user is the owner so the screen can
        //    surface owner-only affordances.
        groupListener = groupRef.addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            _groupName.value = snap.getString("name") ?: "Untitled"

            val ownerId = snap.getString("ownerId")
            _isOwner.value = ownerId != null && ownerId == AuthRepository.currentUid

            @Suppress("UNCHECKED_CAST")
            val uids = (snap.get("memberIds") as? List<String>) ?: emptyList()
            viewModelScope.launch {
                val nameMap = AuthRepository.getUserNames(uids)
                _members.value = uids.map { uid ->
                    Member(
                        id             = uid,
                        name           = nameMap[uid] ?: "Member",
                        sharedMemories = 0,
                        isOnline       = false
                    )
                }
            }
        }

        // 2) Scrapbooks subcollection — newest month first (doc id "YYYY-MM").
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
     * `onDone` so the screen can navigate back. GroupRepository.leaveGroup
     * handles the owner-transfer / delete-if-last-member logic internally.
     */
    fun leaveGroup(onDone: () -> Unit) {
        val groupId = boundGroupId ?: return
        val uid = AuthRepository.currentUid ?: return
        viewModelScope.launch {
            runCatching { GroupRepository.leaveGroup(groupId, uid) }
                .onSuccess { onDone() }
        }
    }

    /**
     * Owner-only: remove a specific member from the group. UI gates this
     * behind isOwner; the repository call still runs without a guard so a
     * misuse would surface as a Firestore rule error (when rules land).
     */
    fun kickMember(memberUid: String) {
        val groupId = boundGroupId ?: return
        if (!_isOwner.value) return
        viewModelScope.launch {
            runCatching { GroupRepository.kickMember(groupId, memberUid) }
        }
    }

    /**
     * Owner-only: delete the entire group. Same caveat as kickMember re: rules.
     * Subcollections (scrapbooks) and Storage photos are not cascaded — a
     * Cloud Function would handle that in production.
     */
    fun deleteGroup(onDone: () -> Unit) {
        val groupId = boundGroupId ?: return
        if (!_isOwner.value) return
        viewModelScope.launch {
            runCatching { GroupRepository.deleteGroup(groupId) }
                .onSuccess { onDone() }
        }
    }

    private fun detachAll() {
        groupListener?.remove();      groupListener = null
        scrapbooksListener?.remove(); scrapbooksListener = null
    }

    override fun onCleared() {
        super.onCleared()
        detachAll()
    }
}
