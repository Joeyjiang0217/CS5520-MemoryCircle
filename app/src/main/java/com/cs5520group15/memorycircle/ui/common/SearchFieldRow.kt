/**
 * What: An editable search input row with a leading search icon and keyboard search action.
 * Who:  Used by AddFriendSearchScreen and FriendsSearchScreen.
 * When: Composed at the top of a search screen where the user types a live query.
 */

package com.cs5520group15.memorycircle.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.theme.AccentGreen
import com.cs5520group15.memorycircle.ui.theme.Brown
import com.cs5520group15.memorycircle.ui.theme.InkTertiary
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Focused search-field row used at the top of full-screen search overlays.
 *       An OutlinedTextField on the left (auto-focused via the supplied
 *       FocusRequester, search icon as leading affordance, IME action = Search)
 *       and a Cancel TextButton on the right that pops back to the previous
 *       screen. Shared by AddFriendSearchScreen and FriendsSearchScreen.
 * Who: Called by any "active search" screen.
 * When: Rendered once at the top of the overlay.
 */
@Composable
fun SearchFieldRow(
    query:          String,
    onQueryChange:  (String) -> Unit,
    focusRequester: FocusRequester,
    placeholder:    String,
    onSearch:       () -> Unit,
    onCancel:       () -> Unit,
    modifier:       Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = modifier
    ) {
        OutlinedTextField(
            value         = query,
            onValueChange = onQueryChange,
            modifier      = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine    = true,
            shape         = RoundedCornerShape(24.dp),
            placeholder   = {
                Text(
                    text  = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTertiary
                )
            },
            leadingIcon   = {
                Icon(
                    painter            = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint               = Brown
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors          = brandFieldColors()
        )
        TextButton(onClick = onCancel) {
            Text(
                text  = "Cancel",
                style = MaterialTheme.typography.labelLarge,
                color = AccentGreen
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun SearchFieldRowPreview() {
    MemoryCircleTheme {
        SearchFieldRow(
            query          = "Emma",
            onQueryChange  = {},
            focusRequester = remember { FocusRequester() },
            placeholder    = "Search friends",
            onSearch       = {},
            onCancel       = {},
            modifier       = Modifier.padding(16.dp)
        )
    }
}
