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

package bobko.todomail.injector

import android.content.SharedPreferences
import bobko.todomail.util.PrefWriterDslReceiver
import bobko.todomail.util.PrefWriterDslReceiverImpl
import bobko.todomail.util.isTestMode

interface Injector {
    fun createPrefWriterDslReceiver(pref: SharedPreferences, editor: SharedPreferences.Editor): PrefWriterDslReceiver
}

private object InjectorProduction : Injector {
    override fun createPrefWriterDslReceiver(pref: SharedPreferences, editor: SharedPreferences.Editor) =
        PrefWriterDslReceiverImpl(pref, editor)
}

val injector: Injector =
    if (isTestMode) {
        Class.forName("bobko.todomail.injector.InjectorForTests").kotlin.objectInstance!! as Injector
    } else {
        InjectorProduction
    }
