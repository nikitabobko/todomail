package bobko.todomail.model

import android.content.Context
import androidx.activity.ComponentActivity
import bobko.todomail.login.createEmail
import bobko.todomail.pref.SharedPref
import bobko.todomail.pref.intSharedPref
import bobko.todomail.pref.stringSharedPref
import bobko.todomail.util.PrefReaderDslReceiver
import bobko.todomail.util.PrefWriterDslReceiver
import java.util.*
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport

private const val DEFAULT_SMTP_PORT = 25

data class SmtpCredential(
    val smtpServer: String,
    val smtpServerPort: Int,
    val username: String,
    val password: String,
) : EmailCredential() {
    override fun getLabel(context: Context): String = if (username.isEmpty()) "SMTP" else "$username (SMTP)"

    override suspend fun signOut(context: Context) {
        // Nothing to do
    }

    companion object {
        val default get() = SmtpCredential("", DEFAULT_SMTP_PORT, "", "")
    }

    class Pref(index: Int) : SharedPref<SmtpCredential>(null) {
        private val smtpServer by stringSharedPref("", index.toString())
        private val smtpServerPort by intSharedPref(0, index.toString())
        private val smtpUsername by stringSharedPref("", index.toString())
        private val smtpPassword by stringSharedPref("", index.toString())

        override fun PrefWriterDslReceiver.writeImpl(value: SmtpCredential?) {
            smtpServer.write(value?.smtpServer)
            smtpServerPort.write(value?.smtpServerPort)
            smtpUsername.write(value?.username)
            smtpPassword.write(value?.password)
        }

        override fun PrefReaderDslReceiver.read() = SmtpCredential(
            smtpServer.read(),
            smtpServerPort.read(),
            smtpUsername.read(),
            smtpPassword.read(),
        )
    }

    fun sendEmail(
        activity: ComponentActivity,
        to: String,
        subject: String,
        body: String
    ) {
        val prop = Properties().apply {
            this["mail.smtp.host"] = smtpServer
            this["mail.smtp.port"] = smtpServerPort
            this["mail.smtp.auth"] = "true"
            this["mail.smtp.starttls.enable"] = "true"
        }
        val session = object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        }
        Transport.send(createEmail(to, subject, body, Session.getInstance(prop, session)))
    }
}
