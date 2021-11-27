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
