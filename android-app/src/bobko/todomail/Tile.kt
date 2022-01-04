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

package bobko.todomail

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import bobko.todomail.model.StartedFrom

@RequiresApi(Build.VERSION_CODES.N)
class Tile : TileService() {
    override fun onClick() {
        startActivityAndCollapse(MainActivity.getIntent(this, StartedFrom.Tile).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}
