package bobko.todomail

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import bobko.todomail.model.pref.PrefManager
import bobko.todomail.util.writePref
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesTest {
    @Test
    fun test() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.writePref {
            PrefManager.all.forEach {
                // TODO
            }
        }
    }
}

