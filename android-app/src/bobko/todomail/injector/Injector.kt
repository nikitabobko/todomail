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
