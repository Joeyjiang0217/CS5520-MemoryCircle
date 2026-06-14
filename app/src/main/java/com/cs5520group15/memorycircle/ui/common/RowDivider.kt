package com.cs5520group15.memorycircle.ui.common

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cs5520group15.memorycircle.ui.theme.Beige
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

/**
 * What: Thin Beige divider used between settings rows, edit-profile rows, and
 *       notification toggles. Centralised so the divider color stays consistent.
 * Who: Called between any pair of stacked list rows that need a separator.
 * When: Rendered wherever a hairline separator is required.
 */
@Composable
fun RowDivider() {
    HorizontalDivider(color = Beige.copy(alpha = 0.5f))
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F4EE)
@Composable
fun RowDividerPreview() {
    MemoryCircleTheme {
        RowDivider()
    }
}
