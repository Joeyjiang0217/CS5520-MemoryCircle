package com.cs5520group15.memorycircle.ui.friends

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * What: State for the "Add new friend" search overlay. Holds two distinct
 *       strings — the live `query` (what the user is currently typing) and the
 *       committed `submittedQuery` (the query that was active the last time
 *       the user tapped Search on the keyboard). Results are derived from
 *       `submittedQuery` only — live-as-you-type filtering hurt UX when
 *       searching against the broader user pool, so the screen waits for an
 *       explicit submit instead.
 *
 *       Matching logic, per spec:
 *         - If the query looks like a complete email (matches the EmailShape
 *           regex below), do a case-insensitive EXACT match against email.
 *         - Otherwise fall back to a case-insensitive SUBSTRING match against
 *           name. Partial emails ("a@", "@gmail") therefore return nothing,
 *           which is the intended behaviour — adding a friend by their email
 *           requires typing the whole thing.
 * Who: Used by AddFriendSearchScreen.
 * When: Created on first composition; survives config changes.
 */
class AddFriendSearchViewModel : ViewModel() {

    private val _query          = MutableStateFlow("")
    private val _submittedQuery = MutableStateFlow("")

    val query:          StateFlow<String>     = _query
    val submittedQuery: StateFlow<String>     = _submittedQuery

    val users:    StateFlow<List<Friend>>     = FriendsRepository.discoverableUsers
    val friends:  StateFlow<List<Friend>>     = FriendsRepository.friends
    val invited:  StateFlow<Set<String>>      = FriendsRepository.invitedUserIds

    fun onQueryChange(s: String) { _query.value = s }

    /**
     * What: Commits the current live query as the submitted query, which is
     *       what the result list reads from.
     * Who: Called by AddFriendSearchScreen when the user taps Search on the
     *      soft keyboard.
     * When: Per Search-key press.
     */
    fun submit() {
        _submittedQuery.value = _query.value
    }

    /**
     * What: Marks the given user as invited. Idempotent and friend-aware — the
     *       repository drops the call if the target is already a friend or
     *       has already been invited.
     * Who: Called by AddFriendSearchScreen on Add-button tap.
     * When: Per Add tap on a search result row.
     */
    fun invite(userId: String) = FriendsRepository.invite(userId)

    /**
     * What: Per-spec matching, always case-insensitive.
     *         - Complete-email query → exact match against email (applies to
     *           friends and non-friends equally — confirming an already-added
     *           contact by their full address is useful, not confusing).
     *         - Plain text query → username search. For NON-FRIENDS we do
     *           substring matching so the user can discover new people; for
     *           ALREADY-FRIENDS we require a full name match so they don't
     *           muddy substring results on what's primarily an
     *           "add a new friend" screen.
     * Who: Called by AddFriendSearchScreen to derive the visible results.
     * When: Every recomposition while submittedQuery is non-blank.
     */
    fun match(q: String): List<Friend> {
        val needle = q.trim()
        if (needle.isEmpty()) return emptyList()

        if (needle.matches(EmailShape)) {
            return users.value.filter { it.email.equals(needle, ignoreCase = true) }
        }

        // Username branch — strict for friends (exact name), loose for strangers.
        val friendIds = friends.value.map { it.id }.toSet()
        return users.value.filter { u ->
            if (u.id in friendIds) {
                u.name.equals(needle, ignoreCase = true)
            } else {
                u.name.contains(needle, ignoreCase = true)
            }
        }
    }

    companion object {
        // Simple "looks like a real email" heuristic: a local part, an @, a
        // domain part, a dot, and a TLD-ish tail — all free of whitespace and
        // extra @. Not RFC 5322 conformant on purpose; this is just deciding
        // whether to switch the match mode, not validating an address.
        private val EmailShape = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
