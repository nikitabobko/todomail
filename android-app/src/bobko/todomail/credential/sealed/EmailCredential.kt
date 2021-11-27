package bobko.todomail.credential.sealed

import android.content.Context
import bobko.todomail.credential.EmailCredentialType
import bobko.todomail.credential.GoogleCredentialType
import bobko.todomail.credential.SmtpCredentialType
import bobko.todomail.pref.SharedPref
import bobko.todomail.pref.stringSharedPref
import bobko.todomail.util.PrefReaderDslReceiver
import bobko.todomail.util.PrefWriterDslReceiver

sealed class EmailCredential {
    abstract val label: String
    abstract val email: String
    abstract val isEmpty: Boolean

    abstract fun sendEmail(
        context: Context,
        to: String,
        subject: String,
        body: String
    )

    class Pref(private val index: Int) : SharedPref<EmailCredential>(null) {
        private val emailCredentialType by stringSharedPref("", index.toString())

        override fun PrefReaderDslReceiver.read() =
            when (val type = emailCredentialType.read()) {
                "google" -> GoogleEmailCredential.Pref(index).read()
                "smtp" -> SmtpCredential.Pref(index).read()
                else -> error("Unknown type: $type")
            }

        override fun PrefWriterDslReceiver.writeImpl(value: EmailCredential?) {
            return when (value) {
                is GoogleEmailCredential -> {
                    emailCredentialType.write("google")
                    GoogleEmailCredential.Pref(index).write(value)
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

val <T : EmailCredential> T.type: EmailCredentialType<T>
    get() = when (val cred = this as EmailCredential) {
        is GoogleEmailCredential -> GoogleCredentialType as EmailCredentialType<T>
        is SmtpCredential -> SmtpCredentialType.findBySmtpServer(cred) as EmailCredentialType<T>
    }
