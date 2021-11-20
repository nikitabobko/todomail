package bobko.todomail.util

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import bobko.todomail.pref.SharedPref

open class PrefReaderDslReceiver(val pref: SharedPreferences) {
    @MainThread
    open fun <T : Any> SharedPref<T>.read() = with(this@PrefReaderDslReceiver) { read() }

    val <T : Any> SharedPref<T>.liveData: InitializedLiveData<T>
        get() = this@PrefReaderDslReceiver.liveData
}

class PrefWriterDslReceiver(
    pref: SharedPreferences,
    val editor: SharedPreferences.Editor
) : PrefReaderDslReceiver(pref) {
    // Don't convert to property because of https://youtrack.jetbrains.com/issue/KT-49700
    @MainThread
    fun <T : Any> SharedPref<T>.write(value: T?) = with(this@PrefWriterDslReceiver) {
        write(value)
    }
}

inline fun <T> Context.readPref(body: PrefReaderDslReceiver.() -> T) =
    PrefReaderDslReceiver(PreferenceManager.getDefaultSharedPreferences(this)).body()

inline fun <T> Context.writePref(body: PrefWriterDslReceiver.() -> T): T {
    val pref = PreferenceManager.getDefaultSharedPreferences(this)
    val editor = pref.edit()
    return PrefWriterDslReceiver(pref, editor).body().also { editor.apply() }
}
