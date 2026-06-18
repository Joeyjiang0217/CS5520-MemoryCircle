package com.cs5520group15.memorycircle.ui.scrapbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.FirebaseModule
import com.cs5520group15.memorycircle.data.ScrapbookRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * What: UI state for the scrapbook creation screen, serving two modes:
 *       creating a brand-new time point (title + tags + first photo + description)
 *       or joining an existing one (appends only this member's photo + description).
 * Who: Used by ScrapbookScreen.
 * When: Created when the creation screen is displayed.
 */
class ScrapbookViewModel : ViewModel() {

    private val _title            = MutableStateFlow("")
    private val _description      = MutableStateFlow("")
    private val _tags             = MutableStateFlow<List<String>>(emptyList())
    private val _selectedPhotoUri = MutableStateFlow<String?>(null)
    private val _selectedDate     = MutableStateFlow(LocalDate.now())
    private val _takenDates       = MutableStateFlow<Set<LocalDate>>(emptySet())
    private val _isSaving         = MutableStateFlow(false)

    val title:            StateFlow<String>         = _title.asStateFlow()
    val description:      StateFlow<String>         = _description.asStateFlow()
    val tags:             StateFlow<List<String>>   = _tags.asStateFlow()
    val selectedPhotoUri: StateFlow<String?>        = _selectedPhotoUri.asStateFlow()
    val selectedDate:     StateFlow<LocalDate>      = _selectedDate.asStateFlow()
    val takenDates:       StateFlow<Set<LocalDate>> = _takenDates.asStateFlow()
    val isSaving:         StateFlow<Boolean>        = _isSaving.asStateFlow()

    sealed class SaveEvent {
        object Success : SaveEvent()
        data class Error(val message: String) : SaveEvent()
    }

    private val _events = Channel<SaveEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var joinEntryId: String? = null
    private var loaded = false

    val isJoinMode: Boolean get() = joinEntryId != null

    /**
     * Pre-loads title + tags from an existing entry when joining it (title +
     * tags are then read-only). No-op when creating a new time point.
     */
    fun loadIfNeeded(groupId: String, entryId: String?) {
        if (loaded) return
        loaded = true
        joinEntryId = entryId
        if (entryId != null) {
            ScrapbookRepository.entry(groupId, entryId)?.let { existing ->
                _title.value = existing.title
                _tags.value  = existing.tags
            }
        }
        loadTakenDates(groupId)
    }

    /**
     * Loads the days in the current month that already have a post, so the
     * date picker can gray them out.
     */
    private fun loadTakenDates(groupId: String) {
        val scrapbookId = YearMonth.now().toString()
        viewModelScope.launch {
            try {
                val snapshot = FirebaseModule.db.collection("groups").document(groupId)
                    .collection("scrapbooks").document(scrapbookId)
                    .collection("posts")
                    .get().await()
                val zone = ZoneId.systemDefault()
                _takenDates.value = snapshot.documents.mapNotNull { doc ->
                    doc.getTimestamp("date")?.toDate()?.toInstant()?.atZone(zone)?.toLocalDate()
                }.toSet()
            } catch (_: Exception) {
                // Leave taken dates empty on failure.
            }
        }
    }

    fun onTitleChange(value: String)       { _title.value = value }
    fun onDescriptionChange(value: String) { _description.value = value }
    fun onPhotoSelected(uri: String)       { _selectedPhotoUri.value = uri }
    fun onDateSelected(date: LocalDate)    { _selectedDate.value = date }

    fun onAddTag(tag: String) {
        if (tag.isBlank()) return
        _tags.value = _tags.value + "#${tag.removePrefix("#")}"
    }

    fun onRemoveTag(tag: String) {
        _tags.value = _tags.value.filter { it != tag }
    }

    val canSave: Boolean
        get() = _selectedPhotoUri.value != null && (isJoinMode || _title.value.isNotBlank())

    /**
     * Persists the post — creating a brand-new one or appending to an
     * existing one — then signals completion via the events channel.
     * The `today` String param is kept so the screen's call site is unchanged;
     * the real persisted date comes from `_selectedDate`.
     */
    fun save(groupId: String, today: String) {
        if (_selectedPhotoUri.value == null) return
        if (_isSaving.value) return
        val photoUri    = _selectedPhotoUri.value!!
        val description = _description.value.trim()
        val joinId      = joinEntryId
        viewModelScope.launch {
            _isSaving.value = true
            try {
                ScrapbookRepository.addPost(
                    groupId          = groupId,
                    title            = _title.value.trim(),
                    tags             = _tags.value,
                    description      = description,
                    selectedPhotoUri = photoUri,
                    date             = _selectedDate.value,
                    joinPostId       = joinId
                )
                _events.send(SaveEvent.Success)
            } catch (e: Exception) {
                _events.send(
                    SaveEvent.Error(e.message ?: "Couldn't save your memory. Please try again.")
                )
            } finally {
                _isSaving.value = false
            }
        }
    }
}
