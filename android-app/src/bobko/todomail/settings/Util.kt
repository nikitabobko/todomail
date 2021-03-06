/*
 * Copyright (C) 2022 Nikita Bobko
 *
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

package bobko.todomail.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import bobko.todomail.model.StartedFrom
import bobko.todomail.pref.SharedPref
import bobko.todomail.theme.EmailTodoTheme
import bobko.todomail.util.doesSupportTiles
import bobko.todomail.util.observeAsState
import bobko.todomail.util.readPref
import bobko.todomail.util.writePref

@Composable
fun Fragment.SettingsScreen(
    title: String,
    rootSettingsScreen: Boolean = false,
    content: @Composable () -> Unit
) {
    EmailTodoTheme {
        Surface {
            Column {
                TopAppBar(
                    modifier = Modifier.fillMaxWidth(),
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (rootSettingsScreen) {
                                    requireActivity().finish()
                                } else {
                                    findNavController().navigateUp()
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.ArrowBack, "")
                        }
                    }
                )
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwitchOrCheckBoxItem(
    text: String,
    checked: Boolean,
    onChecked: (() -> Unit)?,
    description: String? = null,
    useCheckBox: Boolean = false
) {
    ListItem(
        trailing = {
            if (useCheckBox) {
                Checkbox(checked = checked, onCheckedChange = null, enabled = onChecked != null)
            } else {
                Switch(checked = checked, onCheckedChange = null, enabled = onChecked != null)
            }
        },
        modifier = run {
            if (onChecked != null) Modifier.clickable(onClick = onChecked)
            else Modifier
        }
    ) {
        val disabledContentColor: Color = MaterialTheme.colors.onSurface
            .copy(alpha = ContentAlpha.disabled)
        val content = @Composable {
            if (description != null) {
                Column {
                    Text(text)
                    Text(
                        description,
                        color = disabledContentColor,
                        style = MaterialTheme.typography.caption
                    )
                }
            } else {
                Text(text)
            }
        }
        if (onChecked == null) {
            CompositionLocalProvider(LocalContentAlpha provides disabledContentColor.alpha) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
fun WhenTheAppIsStartedFromSection(
    whenStartedFrom: List<Pair<StartedFrom, SharedPref<Boolean>>>
) {
    whenStartedFrom.forEach { (startedFrom, prefKey) ->
        if (startedFrom == StartedFrom.Tile && !doesSupportTiles) {
            SwitchOrCheckBoxItem(
                startedFrom.text,
                description = "Tiles are available only since Android N",
                checked = false,
                onChecked = null,
                useCheckBox = true
            )
        } else {
            val context = LocalContext.current
            val checked by context.readPref { prefKey.liveData }.observeAsState()
            SwitchOrCheckBoxItem(
                startedFrom.text,
                checked = checked,
                onChecked = {
                    context.writePref {
                        prefKey.write(!checked)
                    }
                },
                useCheckBox = true
            )
        }
    }
}
