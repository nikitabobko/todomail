/*
 * This file is part of Todomail.
 *
 * Todomail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Todomail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Todomail. If not, see <https://www.gnu.org/licenses/>.
 */

package bobko.todomail.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Copied from [androidx.compose.material.OutlinedTextFieldTopPadding]
 */
val OutlinedTextFieldTopPadding get() = 8.dp

@Composable
fun Spinner(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = label,
        onValueChange = {},
        enabled = false,
        modifier = modifier.clickable(enabled = onClick != null) {
            onClick?.invoke()
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            disabledLeadingIconColor = MaterialTheme.colors.onBackground,
            disabledTextColor = MaterialTheme.colors.onBackground,
            disabledTrailingIconColor = MaterialTheme.colors.onBackground
        )
    )
}

@Composable
fun CenteredRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, content = content)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TextDivider(text: String) {
    ListItem(modifier = Modifier.height(32.dp)) {
        Text(text, color = MaterialTheme.colors.primary, style = MaterialTheme.typography.subtitle2)
    }
}
