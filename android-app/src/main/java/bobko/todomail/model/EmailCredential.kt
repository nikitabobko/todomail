package bobko.todomail.model

import androidx.activity.ComponentActivity
import bobko.todomail.pref.SharedPref
import bobko.todomail.pref.stringSharedPref
import bobko.todomail.util.PrefReaderDslReceiver
import bobko.todomail.util.PrefWriterDslReceiver

sealed class EmailCredential {
    abstract val label: String

    abstract suspend fun sendEmail(
        activity: ComponentActivity,
        to: String,
        subject: String,
        body: String
    )

    class Pref(private val index: Int) : SharedPref<EmailCredential>(null) {
        private val emailCredentialType by stringSharedPref("", index.toString())

        override fun PrefReaderDslReceiver.read() =
            when (val type = emailCredentialType.read()) {
                "google" -> GoogleEmailCredential
                "smtp" -> SmtpCredential.Pref(index).read()
                else -> error("Unknown type: $type")
            }

        override fun PrefWriterDslReceiver.write(value: EmailCredential?) {
            return when (value) {
                GoogleEmailCredential -> {
                    emailCredentialType.write("google")
                }
                is SmtpCredential -> {
                    emailCredentialType.write("smtp")
                    SmtpCredential.Pref(index).write(value)
                }
                null -> {
                    emailCredentialType.write(null)
                }
            }
        }
    }
}
