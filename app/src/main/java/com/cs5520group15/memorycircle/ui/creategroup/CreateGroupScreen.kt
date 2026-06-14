package com.cs5520group15.memorycircle.ui.creategroup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.model.Friend
import com.cs5520group15.memorycircle.ui.common.AvatarCircle
import com.cs5520group15.memorycircle.ui.common.EmptyHint
import com.cs5520group15.memorycircle.ui.common.MemoryCircleTopBar
import com.cs5520group15.memorycircle.ui.common.PrimaryButton
import com.cs5520group15.memorycircle.ui.common.SecondaryOutlinedButton
import com.cs5520group15.memorycircle.ui.theme.*

/**
 * What: Contact picker for the "create a new group" and "invite new members"
 *       flows. Header with back arrow and contextual title; a search bar that
 *       previews picked avatars on its left and flips to an active TextField
 *       when tapped; a scrollable contact list with a checkbox on every row;
 *       and a sticky bottom CTA that switches from a disabled-looking outlined
 *       button (no selection) to a filled "Create Now (N)" / "Invite Now (N)"
 *       the moment any row is checked. Confirming mints a new group + routes
 *       to its empty timeline; in invite mode, confirming hands the picked
 *       ids back to the caller (member-add wiring lands later).
 *
 *       Active search flow (covered in screenshots 1-3):
 *         tap bar → keyboard up, query empty → full contact list still visible.
 *         type → result list replaces the contact list; already-selected
 *           contacts in the results render greyed out and non-tappable so the
 *           user can't try to "re-add" someone they've already picked.
 *         tap result → that contact is added to the selection, search closes,
 *           query clears, and the contact list reappears (now with the new
 *           checkmark in place).
 *         system back / "Cancel" while typing → exits search without leaving.
 * Who: Called by MemoryCircleNavigation for the CreateGroup route.
 * When: Reached from the "+" FAB on HomeScreen (new-group mode) or from the
 *       invite "+" tile on GroupDetailScreen (invite mode).
 *
 * @param isInviteMode true = invite UX (title "Invite New Members", CTA
 *                     "Invite Now (N)"); false = new-group UX.
 * @param onCreated    invoked with the freshly minted groupId once the user
 *                     confirms in NEW-GROUP mode. Ignored in invite mode.
 * @param onInvite     invoked with the picked member ids once the user
 *                     confirms in INVITE mode. Ignored in new-group mode.
 */
@Composable
fun CreateGroupScreen(
    onBack:       () -> Unit,
    isInviteMode: Boolean = false,
    onCreated:    (String) -> Unit = {},
    onInvite:     (List<String>) -> Unit = {},
    viewModel:    CreateGroupViewModel = viewModel()
) {
    val contacts    by viewModel.contacts.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val query       by viewModel.query.collectAsStateWithLifecycle()

    val selectedContacts = remember(contacts, selectedIds) {
        contacts.filter { it.id in selectedIds }
    }

    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current

    // Auto-focus the TextField the moment the user activates search.
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) focusRequester.requestFocus()
    }

    // System back exits search mode first instead of unwinding the screen so the
    // user doesn't lose their selection by accident.
    BackHandler(enabled = isSearchActive) {
        keyboard?.hide()
        viewModel.clearQuery()
        isSearchActive = false
    }

    val title    = if (isInviteMode) "Invite New Members" else "New Group"
    val ctaLabel = if (isInviteMode) "Invite Now"         else "Create Now"

    val onConfirm: () -> Unit = {
        if (isInviteMode) {
            onInvite(selectedIds.toList())
        } else {
            viewModel.createGroup()?.let(onCreated)
        }
    }

    Scaffold(
        containerColor = Cream,
        topBar = {
            MemoryCircleTopBar(
                title    = title,
                showBack = true,
                onBack   = onBack
            )
        },
        bottomBar = {
            // CTA stays pinned even while the search keyboard is up — the user
            // should always be able to confirm their current selection without
            // first having to dismiss search.
            BottomCta(
                selectedCount = selectedIds.size,
                ctaLabel      = ctaLabel,
                onConfirm     = onConfirm
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isSearchActive) {
                ActiveSearchRow(
                    query          = query,
                    onQueryChange  = viewModel::onQueryChange,
                    selected       = selectedContacts,
                    focusRequester = focusRequester,
                    onClear        = viewModel::clearQuery,
                    modifier       = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            } else {
                TapSearchRow(
                    selected = selectedContacts,
                    onClick  = { isSearchActive = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            HorizontalDivider(color = Beige.copy(alpha = 0.5f))

            // Body: full contact list in default state, search results when the
            // user has actively typed something. While search is active with an
            // empty query, we still show the full list so the picker doesn't
            // visually collapse the moment focus enters.
            val showResults = isSearchActive && query.isNotBlank()
            if (showResults) {
                val results = viewModel.match(query)
                if (results.isEmpty()) {
                    EmptyHint(text = "No contacts matched \"${query.trim()}\".")
                } else {
                    SearchResults(
                        results     = results,
                        selectedIds = selectedIds,
                        onPick      = { id ->
                            viewModel.toggle(id)
                            viewModel.clearQuery()
                            keyboard?.hide()
                            isSearchActive = false
                        }
                    )
                }
            } else {
                ContactsList(
                    contacts    = contacts,
                    selectedIds = selectedIds,
                    onToggle    = viewModel::toggle,
                    modifier    = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * What: The default search bar — tap target only. Picked-avatar previews line
 *       up at the left so the user can see their selection even while the
 *       contact list is scrolled; the previews are capped at 4 with an
 *       inline "+N" overflow chip so the row can't outgrow one line.
 * Who: Called by CreateGroupScreen when isSearchActive is false.
 * When: Visible in the default state.
 */
@Composable
private fun TapSearchRow(
    selected: List<Friend>,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxPreview = 4
    val previews   = selected.take(maxPreview)
    val overflow   = (selected.size - maxPreview).coerceAtLeast(0)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WhiteCard)
            .border(1.dp, Beige.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        SelectedAvatarPreviews(previews = previews, overflow = overflow)
        if (previews.isEmpty()) {
            Icon(
                painter            = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint               = Brown,
                modifier           = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text  = "Search",
            style = MaterialTheme.typography.bodyLarge,
            color = InkTertiary
        )
    }
}

/**
 * What: The active search bar — same chrome as TapSearchRow but with an
 *       editable BasicTextField in place of the static "Search" placeholder
 *       and a small grey clear button on the right when the query is non-empty.
 *       BasicTextField is used rather than OutlinedTextField so the field
 *       inherits the parent Row's height + background without the chunky
 *       Material outline visible inside the already-bordered container.
 * Who: Called by CreateGroupScreen when isSearchActive is true.
 * When: Visible while the user has tapped into search.
 */
@Composable
private fun ActiveSearchRow(
    query:          String,
    onQueryChange:  (String) -> Unit,
    selected:       List<Friend>,
    focusRequester: FocusRequester,
    onClear:        () -> Unit,
    modifier:       Modifier = Modifier
) {
    val maxPreview = 4
    val previews   = selected.take(maxPreview)
    val overflow   = (selected.size - maxPreview).coerceAtLeast(0)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WhiteCard)
            .border(1.dp, Beige.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        SelectedAvatarPreviews(previews = previews, overflow = overflow)

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text  = "Search",
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkTertiary
                )
            }
            BasicTextField(
                value           = query,
                onValueChange   = onQueryChange,
                singleLine      = true,
                cursorBrush     = SolidColor(Brown),
                textStyle       = TextStyle(
                    color    = Ink,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* live filter — nothing to commit */ }),
                modifier        = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }

        if (query.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            ClearQueryButton(onClick = onClear)
        }
    }
}

/**
 * What: Renders the avatar-preview run shared by both search bar variants.
 *       Extracted so the two callers stay in visual lock-step (avatar size,
 *       spacing, overflow chip treatment).
 * Who: Called by TapSearchRow and ActiveSearchRow.
 * When: Rendered at the left of either search bar when at least one contact
 *       is selected.
 */
@Composable
private fun SelectedAvatarPreviews(
    previews: List<Friend>,
    overflow: Int
) {
    if (previews.isEmpty()) return
    previews.forEach { friend ->
        AvatarCircle(name = friend.name, size = 28.dp)
        Spacer(modifier = Modifier.width(6.dp))
    }
    if (overflow > 0) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Beige.copy(alpha = 0.6f))
        ) {
            Text(
                text  = "+$overflow",
                style = MaterialTheme.typography.labelSmall,
                color = Brown
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
    }
}

/**
 * What: Small round clear-query button on the right of the active search bar.
 *       Same visual family as DeclineCircleButton (neutral grey background +
 *       ic_close glyph) but with a smaller tap target since it lives inside
 *       a text input row.
 * Who: Called by ActiveSearchRow when the query is non-empty.
 * When: Rendered while the user has typed something.
 */
@Composable
private fun ClearQueryButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(GraySoft)
            .clickable { onClick() }
    ) {
        Icon(
            painter            = painterResource(R.drawable.ic_close),
            contentDescription = "Clear search",
            tint               = InkSecondary,
            modifier           = Modifier.size(12.dp)
        )
    }
}

/**
 * What: Scrollable contact list — the default body of the screen. Every row
 *       is tappable in its entirety so the user can flip the selection
 *       without aiming at the checkbox.
 * Who: Called by CreateGroupScreen.
 * When: Rendered in the default state, and also while search is active with
 *       an empty query.
 */
@Composable
private fun ContactsList(
    contacts:    List<Friend>,
    selectedIds: Set<String>,
    onToggle:    (String) -> Unit,
    modifier:    Modifier = Modifier
) {
    LazyColumn(
        modifier       = modifier,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(contacts, key = { it.id }) { contact ->
            ContactRow(
                contact  = contact,
                selected = contact.id in selectedIds,
                onClick  = { onToggle(contact.id) }
            )
        }
    }
}

/**
 * What: Live search results body. Replaces ContactsList while the user has
 *       typed something. Each row mirrors the contact-list visual but uses a
 *       different click semantic: tapping a non-selected result picks the
 *       contact (and the parent closes search). Already-selected contacts
 *       render greyed out and non-tappable so the user can see "yes I
 *       already grabbed them" without the option to try again.
 * Who: Called by CreateGroupScreen while isSearchActive and query non-blank.
 * When: Per recomposition with a non-empty match list.
 */
@Composable
private fun SearchResults(
    results:     List<Friend>,
    selectedIds: Set<String>,
    onPick:      (String) -> Unit
) {
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(results, key = { it.id }) { friend ->
            val alreadyPicked = friend.id in selectedIds
            ResultRow(
                friend  = friend,
                disabled = alreadyPicked,
                onClick  = { if (!alreadyPicked) onPick(friend.id) }
            )
        }
    }
}

/**
 * What: One contact row used by the default ContactsList. Branded selection
 *       circle on the left, avatar + name in the middle. The whole row is
 *       the tap target; the checkbox is purely visual feedback.
 * Who: Called by ContactsList.
 * When: Rendered per contact.
 */
@Composable
private fun ContactRow(
    contact:  Friend,
    selected: Boolean,
    onClick:  () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        SelectionCheckbox(selected = selected, disabled = false)
        Spacer(modifier = Modifier.width(12.dp))
        AvatarCircle(name = contact.name, size = 44.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text     = contact.name,
            style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color    = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * What: One row in the search results list. Same anatomy as ContactRow but
 *       greys out the entire row (avatar tinted via alpha, name in InkTertiary,
 *       checkbox locked) when the contact is already in the selection.
 * Who: Called by SearchResults.
 * When: Rendered per match while the user is typing.
 */
@Composable
private fun ResultRow(
    friend:   Friend,
    disabled: Boolean,
    onClick:  () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !disabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        SelectionCheckbox(selected = disabled, disabled = disabled)
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.alpha(if (disabled) 0.45f else 1f)) {
            AvatarCircle(name = friend.name, size = 44.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = friend.name,
                style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color    = if (disabled) InkTertiary else Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (disabled) {
                Text(
                    text  = "Already added",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTertiary
                )
            }
        }
    }
}

/**
 * What: Branded selection circle drawn as a small Box rather than the default
 *       Material Checkbox so the visual stays inside the brand palette
 *       (AccentGreen / Brown) instead of jumping to Material blue.
 *       When `disabled = true`, the fill switches to BrownDisabled and the
 *       border drops a tone so the row reads as "locked, already taken".
 * Who: Called by ContactRow and ResultRow.
 * When: Rendered as the leading affordance of every contact / result row.
 */
@Composable
private fun SelectionCheckbox(selected: Boolean, disabled: Boolean) {
    val fill   = when {
        disabled -> BrownDisabled
        selected -> AccentGreen
        else     -> Cream
    }
    val border = when {
        disabled -> BrownDisabled
        selected -> AccentGreen
        else     -> Brown.copy(alpha = 0.55f)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(fill)
            .border(width = 1.5.dp, color = border, shape = CircleShape)
    ) {
        if (selected) {
            Text(
                text  = "✓",
                style = MaterialTheme.typography.labelSmall,
                color = Cream
            )
        }
    }
}

/**
 * What: Sticky CTA at the foot of the screen. With nothing selected the button
 *       reads as a disabled-looking outlined affordance — the user can see
 *       what's coming but the row is non-interactive. The moment any contact
 *       is picked it switches to a filled PrimaryButton labelled with the
 *       provided ctaLabel + "(N)" so the count updates live.
 * Who: Called by CreateGroupScreen's Scaffold.bottomBar.
 * When: Rendered whenever isSearchActive is false.
 */
@Composable
private fun BottomCta(
    selectedCount: Int,
    ctaLabel:      String,
    onConfirm:     () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cream)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (selectedCount == 0) {
            SecondaryOutlinedButton(
                label        = ctaLabel,
                onClick      = {},
                borderColor  = Brown.copy(alpha = 0.3f),
                contentColor = InkTertiary
            )
        } else {
            PrimaryButton(
                label   = "$ctaLabel ($selectedCount)",
                onClick = onConfirm
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun CreateGroupScreenPreview() {
    MemoryCircleTheme {
        CreateGroupScreen(
            onBack    = {},
            onCreated = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun CreateGroupScreenInvitePreview() {
    MemoryCircleTheme {
        CreateGroupScreen(
            onBack       = {},
            isInviteMode = true,
            onInvite     = {}
        )
    }
}
