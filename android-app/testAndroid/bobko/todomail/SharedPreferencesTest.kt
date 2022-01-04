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

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import bobko.todomail.credential.UniqueEmailCredential
import bobko.todomail.credential.sealed.EmailCredential
import bobko.todomail.credential.sealed.GoogleEmailCredential
import bobko.todomail.credential.sealed.SmtpCredential
import bobko.todomail.model.EmailTemplate
import bobko.todomail.model.EmailTemplateRaw
import bobko.todomail.model.pref.PrefManager
import bobko.todomail.pref.SharedPref
import bobko.todomail.util.PrefWriterDslReceiverForTest
import bobko.todomail.util.writePref
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesTest {
    @Test
    fun testPrefKeysAreStable() = onUiThread {
        clearSharedPrefs()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PrefWriterDslReceiverForTest.checkDuplicates = true
        val all = listOf(
            BooleanSharedPrefTester(PrefManager.prefillWithClipboardWhenStartedFromLauncher),
            BooleanSharedPrefTester(PrefManager.prefillWithClipboardWhenStartedFromTile),
            StringSharedPrefTester(PrefManager.todoDraft),
            EmailTemplatesTester,
        )
        all.forEach { it.writeValue(context) }
        val actualKeys = PreferenceManager.getDefaultSharedPreferences(context).all.keys.sorted().joinToString("\n")
        Assert.assertEquals(
            """
                emailCredentialType0
                emailCredentialType1
                emailTemplateCredentialId0
                emailTemplateCredentialId1
                emailTemplateId0
                emailTemplateId1
                emailTemplateLabel0
                emailTemplateLabel1
                emailTemplateSendTo0
                emailTemplateSendTo1
                googleAccessToken1
                googleAccountId1
                googleEmail1
                googleRefreshToken1
                prefillWithClipboardWhenStartedFromLauncher
                prefillWithClipboardWhenStartedFromTile
                sizeEmailTemplate
                sizeUniqueEmailCredential
                smtpPassword0
                smtpServer0
                smtpServerPort0
                smtpUsername0
                todoDraft
                uniqueCredentialId
                uniqueCredentialId0
                uniqueCredentialId1
                uniqueEmailTemplateId
            """.trimIndent(),
            actualKeys
        )
    }
}

sealed class SharedPrefTester<T : Any>(val sharedPref: SharedPref<T>) {
    abstract fun writeValue(context: Context)
}

class BooleanSharedPrefTester(sharedPref: SharedPref<Boolean>) : SharedPrefTester<Boolean>(sharedPref) {
    override fun writeValue(context: Context) = context.writePref { sharedPref.write(true) }
}

class StringSharedPrefTester(sharedPref: SharedPref<String>) : SharedPrefTester<String>(sharedPref) {
    override fun writeValue(context: Context) = context.writePref { sharedPref.write("TEST") }
}

object EmailTemplatesTester : SharedPrefTester<List<EmailTemplateRaw>>(EmailTemplate.All) {
    override fun writeValue(context: Context) {
        check(EmailCredential::class.sealedSubclasses.size == 2) {
            "Please, add new subclasses to this test. Found classes: " + EmailCredential::class.sealedSubclasses.joinToString()
        }
        context.writePref {
            sharedPref.write(
                listOf(
                    EmailTemplate.new(
                        "Label",
                        "SendTo@example.com",
                        UniqueEmailCredential.new(SmtpCredential.default, context),
                        context
                    ),
                    EmailTemplate.new(
                        "Google Cred",
                        "Sendto@google.com",
                        UniqueEmailCredential.new(GoogleEmailCredential.newTestInstance(), context),
                        context
                    )
                )
            )
        }
    }
}
