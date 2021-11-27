package bobko.todomail

import android.app.Application
import android.content.Intent
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import bobko.todomail.model.pref.PrefManager
import bobko.todomail.util.writePref
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [bobko.todomail.MainActivityViewModel]
 */
@RunWith(AndroidJUnit4::class)
class MainActivityViewModelTest {
    @Test
    fun sharedTextShouldBePrioritizedOverDraftUnitTest() {
        preventMainActivityEarlyExit()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.writePref {
            PrefManager.todoDraft.write("draft text")
        }
        onUiThread {
            val viewModel = MainActivityViewModel(context.applicationContext as Application)
            val expected = "expected text"
            viewModel.prefillSharedText(expected)
            Assert.assertEquals(expected, viewModel.todoTextDraft.value.text.trim())
        }
    }

    @Test
    fun sharedTextShouldBePrioritizedOverDraftIntegrationTest() {
        preventMainActivityEarlyExit()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.writePref {
            PrefManager.todoDraft.write("draft value")
        }
        val expected = "expected text"
        launchActivity<MainActivity>(
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, expected)
            }
        ).onActivity {
            Assert.assertEquals(expected, it.viewModel.todoTextDraft.value.text.trim())
        }
    }
}
