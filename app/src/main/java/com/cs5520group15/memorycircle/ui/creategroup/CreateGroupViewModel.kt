package com.cs5520group15.memorycircle.ui.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.AuthRepository
import com.cs5520group15.memorycircle.data.FirebaseModule
import com.cs5520group15.memorycircle.data.FriendsRepository
import com.cs5520group15.memorycircle.data.Result
import com.cs5520group15.memorycircle.model.Friend
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.YearMonth

/**
 * What: Drives the WeChat-style "create a new group" contact picker. Holds the
 *       set of selected member ids and the live search query; exposes the
 *       friend pool from FriendsRepository so the rows the user picks from
 *       stay in sync with the rest of the app.
 *
 *       Selection state is `Set<String>` rather than `List<String>` so toggling
 *       is O(log n) and idempotent — re-tapping a row that's already selected
 *       removes it, which is the universal contact-picker convention.
 *
 *       Creating commits a real Firestore document at groups/{groupId} with
 *       memberIds = picked friends + the current user as owner, plus an empty
 *       scrapbook for the current month so the timeline has somewhere to land.
 * Who: Used by CreateGroupScreen.
 * When: Created when the screen first composes; discarded when the user
 *       confirms (the screen pops off the back stack via popUpTo inclusive).
 */
class CreateGroupViewModel : ViewModel() {

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _query       = MutableStateFlow("")
    private val _isLoading   = MutableStateFlow(false)
    private val _groupName   = MutableStateFlow("")

    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()
    val query:       StateFlow<String>      = _query.asStateFlow()
    val isLoading:   StateFlow<Boolean>     = _isLoading.asStateFlow()
    val groupName:   StateFlow<String>      = _groupName.asStateFlow()

    /** Source of contacts the picker offers — the user's existing friend list. */
    val contacts: StateFlow<List<Friend>> = FriendsRepository.friends

    sealed class CreateGroupEvent {
        data class ShowSnackbar(val message: String) : CreateGroupEvent()
        data class Created(val groupId: String)      : CreateGroupEvent()
    }

    private val _events = Channel<CreateGroupEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /**
     * What: Flips the membership of `userId` in the selected set.
     * Who: Called when the user taps a contact row or its checkbox.
     */
    fun toggle(userId: String) {
        val current = _selectedIds.value
        _selectedIds.value = if (userId in current) current - userId else current + userId
    }

    fun onQueryChange(value: String) { _query.value = value }
    fun clearQuery()                 { _query.value = "" }

    fun onGroupNameChange(value: String) { _groupName.value = value }

    /**
     * Case-insensitive substring filter against name OR email. Empty / blank
     * queries return an empty list so the caller can simply check isEmpty()
     * to decide whether to render the results section.
     */
    fun match(query: String): List<Friend> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        return contacts.value.filter { c ->
            c.name.contains(needle, ignoreCase = true) ||
            c.email.contains(needle, ignoreCase = true)
        }
    }

    /**
     * What: Writes a new Firestore group with the picked members + current
     *       user as owner, creates a member subdoc per picked friend, and
     *       seeds the current month's scrapbook. Emits Created(groupId) on
     *       success so the screen can navigate to the new timeline.
     *
     *       The card color is not yet user-configurable, so the field is
     *       omitted from the group doc — HomeViewModel reads it with a
     *       "brown" fallback so existing cards render the same. Add a color
     *       picker to the screen later and put "colorType" back into the
     *       write to surface it.
     * Who: Called by CreateGroupScreen when the user taps "Create Now".
     * When: On Create-button click.
     */
    fun onCreateClick() = viewModelScope.launch {
        val uid = AuthRepository.currentUid
        if (uid == null) {
            _events.send(CreateGroupEvent.ShowSnackbar("You must be logged in to create a group"))
            return@launch
        }

        val name = _groupName.value.trim().ifBlank { "New Group" }

        _isLoading.value = true
        try {
            val db = FirebaseModule.db
            val groupRef = db.collection("groups").document()
            val groupId  = groupRef.id

            // memberIds = owner + all picked friends, deduplicated.
            val pickedIds    = _selectedIds.value.toList()
            val allMemberIds = (listOf(uid) + pickedIds).distinct()

            // 1) Group document
            groupRef.set(mapOf(
                "groupId"     to groupId,
                "name"        to name,
                "createdAt"   to FieldValue.serverTimestamp(),
                "ownerId"     to uid,
                "memberIds"   to allMemberIds,
                "memberCount" to allMemberIds.size,
                "memoryCount" to 0
            )).await()

            // 2) Owner member subdoc
            val ownerName = when (val r = AuthRepository.getCurrentUserName()) {
                is Result.Success -> r.data
                else              -> "User"
            }
            groupRef.collection("members").document(uid).set(mapOf(
                "uid"      to uid,
                "name"     to ownerName,
                "role"     to "owner",
                "joinedAt" to FieldValue.serverTimestamp()
            )).await()

            // 3) Member subdoc per picked friend (name from the picker's contact list)
            val pickedFriends = contacts.value.filter { it.id in _selectedIds.value }
            pickedFriends.forEach { friend ->
                groupRef.collection("members").document(friend.id).set(mapOf(
                    "uid"      to friend.id,
                    "name"     to friend.name,
                    "role"     to "member",
                    "joinedAt" to FieldValue.serverTimestamp()
                )).await()
            }

            // 4) Initial scrapbook for the current month
            val scrapbookId = YearMonth.now().toString()
            groupRef.collection("scrapbooks").document(scrapbookId).set(mapOf(
                "scrapbookId" to scrapbookId,
                "postCount"   to 0,
                "createdAt"   to FieldValue.serverTimestamp(),
                "updatedAt"   to FieldValue.serverTimestamp()
            )).await()

            _isLoading.value = false
            _events.send(CreateGroupEvent.Created(groupId))
        } catch (e: Exception) {
            _isLoading.value = false
            _events.send(CreateGroupEvent.ShowSnackbar(e.message ?: "Failed to create group"))
        }
    }
}
