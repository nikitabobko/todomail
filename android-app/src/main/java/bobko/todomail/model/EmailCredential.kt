package bobko.todomail.model

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import bobko.todomail.R
import bobko.todomail.pref.SharedPref
import bobko.todomail.pref.stringSharedPref
import bobko.todomail.settings.DefaultEmailIcon
import bobko.todomail.util.PrefReaderDslReceiver
import bobko.todomail.util.PrefWriterDslReceiver

sealed class EmailCredential : ComposeIconProvider {
    abstract fun getLabel(context: Context): String

    @Composable
    final override fun Icon() {
        when (this) {
            GoogleEmailCredential -> {
                Icon(
                    painterResource(R.drawable.google_logo),
                    "Google logo",
                    modifier = Modifier.size(emailIconSize),
                    tint = Color.Unspecified
                )
            }
            is SmtpCredential -> {
                KnownSmtpCredential.findBySmtpServer(this)?.Icon() ?: DefaultEmailIcon()
            }
            else -> error("")
        }
    }

//    abstract fun sendEmail(
//        activity: ComponentActivity,
//        to: String,
//        subject: String,
//        body: String
//    )

    abstract suspend fun signOut(context: Context)

    class Pref(private val index: Int) : SharedPref<EmailCredential>(null) {
        private val emailCredentialType by stringSharedPref("", index.toString())

        override fun PrefReaderDslReceiver.read() =
            when (val type = emailCredentialType.read()) {
                "google" -> GoogleEmailCredential
                "smtp" -> SmtpCredential.Pref(index).read()
                else -> error("Unknown type: $type")
            }

        override fun PrefWriterDslReceiver.writeImpl(value: EmailCredential?) {
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
