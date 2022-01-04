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

package bobko.todomail.model

import bobko.todomail.model.pref.PrefManager
import bobko.todomail.pref.SharedPref

enum class StartedFrom(
    val text: String,
    val prefillPrefKey: SharedPref<Boolean>?,
) {
    Launcher(
        "started from Launcher",
        PrefManager.prefillWithClipboardWhenStartedFromLauncher,
    ),
    Tile(
        "started from Tile",
        PrefManager.prefillWithClipboardWhenStartedFromTile,
    ),
    Sharesheet(
        "started from Sharesheet",
        null,
    )
}
