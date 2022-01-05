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

package bobko.todomail

import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import bobko.todomail.credential.UniqueEmailCredential
import bobko.todomail.credential.sealed.SmtpCredential
import bobko.todomail.model.EmailTemplate
import bobko.todomail.util.writePref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun <T> onUiThread(block: suspend () -> T) = runBlocking {
    withContext(Dispatchers.Main) {
        block()
    }
}

fun clearSharedPrefs() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    sharedPref.edit().also { editor ->
        sharedPref.all.keys.forEach { key -> editor.remove(key) }
        editor.commit()
    }
}

fun preventMainActivityEarlyExit() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    clearSharedPrefs()
    onUiThread {
        context.writePref {
            EmailTemplate.All.write(
                listOf(
                    EmailTemplate.new(
                        "Prevent MainActivity Exit",
                        "",
                        UniqueEmailCredential.new(SmtpCredential.default, context),
                        context
                    )
                )
            )
        }
    }
}
