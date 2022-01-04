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

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import bobko.todomail.injector.injector
import bobko.todomail.pref.SharedPref

open class PrefReaderDslReceiverImpl(override val pref: SharedPreferences) : PrefReaderDslReceiver {
    @MainThread
    override fun <T : Any> SharedPref<T>.read() = with(this@PrefReaderDslReceiverImpl) { read() }

    override val <T : Any> SharedPref<T>.liveData: InitializedLiveData<T>
        get() = this@PrefReaderDslReceiverImpl.liveData
}

interface PrefReaderDslReceiver {
    val pref: SharedPreferences
    fun <T : Any> SharedPref<T>.read(): T
    val <T : Any> SharedPref<T>.liveData: InitializedLiveData<T>
}

interface PrefWriterDslReceiver : PrefReaderDslReceiver {
    fun putString(key: String, value: String?)
    fun <T : Any> SharedPref<T>.write(value: T?)
}

class PrefWriterDslReceiverImpl(
    pref: SharedPreferences,
    private val editor: SharedPreferences.Editor
) : PrefReaderDslReceiverImpl(pref), PrefWriterDslReceiver {
    override fun putString(key: String, value: String?) {
        editor.putString(key, value)
    }

    // Don't convert to property because of https://youtrack.jetbrains.com/issue/KT-49700
    @MainThread
    override fun <T : Any> SharedPref<T>.write(value: T?) = with(this@PrefWriterDslReceiverImpl) {
        write(value)
    }
}

fun <T> Context.readPref(body: PrefReaderDslReceiver.() -> T) =
    PrefReaderDslReceiverImpl(PreferenceManager.getDefaultSharedPreferences(this)).body()

@SuppressLint("ApplySharedPref")
fun <T> Context.writePref(body: PrefWriterDslReceiver.() -> T): T {
    val pref = PreferenceManager.getDefaultSharedPreferences(this)
    val editor = pref.edit()
    return injector.createPrefWriterDslReceiver(pref, editor).body().also { editor.commit() }
}
