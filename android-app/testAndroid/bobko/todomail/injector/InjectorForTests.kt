package bobko.todomail.injector

import android.content.SharedPreferences
import bobko.todomail.util.PrefWriterDslReceiverForTest
import bobko.todomail.util.PrefWriterDslReceiverImpl

@Suppress("unused") // Used via reflection
object InjectorForTests : Injector {
    override fun createPrefWriterDslReceiver(pref: SharedPreferences, editor: SharedPreferences.Editor) =
        PrefWriterDslReceiverForTest(PrefWriterDslReceiverImpl(pref, editor))
}
