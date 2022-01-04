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

package bobko.todomail.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun EmailTodoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()
    MaterialTheme(
        colors = run {
            if (darkTheme) {
                darkColors(
                    primary = Color(0xff1ca15f),
                    secondaryVariant = Color(0xff1ca15f), // Switch checked color
                    secondary = Color(0xff1ca15f) // Checkbox color
                )
            } else {
                lightColors(
                    primary = Color(0xff1ca15f),
                    secondaryVariant = Color(0xff1ca15f), // Switch checked color
                    secondary = Color(0xff1ca15f) // Checkbox color
                )
            }
        },
        content = {
            val color = MaterialTheme.colors.primarySurface
            SideEffect {
                systemUiController.setSystemBarsColor(color = color)
            }
            content()
        }
    )
}