package bobko.todomail.model

import androidx.activity.ComponentActivity
import bobko.todomail.pref.ListSharedPref
import bobko.todomail.pref.SharedPref
import bobko.todomail.pref.intSharedPref
import bobko.todomail.pref.stringSharedPref
import bobko.todomail.util.*

typealias EmailTemplateRaw = EmailTemplate<*>

data class EmailTemplate<out T : EmailCredential>(
    val label: String,
    val sendTo: String,
    val uniqueCredential: UniqueEmailCredential<T>
) {
    object All : SharedPref<List<EmailTemplateRaw>>(null) {
        private val uniqueSuffix get() = EmailTemplate::class.simpleName!!

        override fun PrefWriterDslReceiver.write(value: List<EmailTemplateRaw>?) {
            val idToCredential =
                value?.associate { it.uniqueCredential.id to it.uniqueCredential } ?: emptyMap()
            UniqueEmailCredential.All.write(value?.map { it.uniqueCredential })
            ListSharedPref(null, uniqueSuffix) { Pref(it, idToCredential) }.write(value)
        }

        override fun PrefReaderDslReceiver.read(): List<EmailTemplateRaw> {
            val idToCredential =
                UniqueEmailCredential.All.read().associateBy { it.id }
            return ListSharedPref(null, uniqueSuffix) { Pref(it, idToCredential) }.read()
        }
    }

    private class Pref(index: Int, val idToCredential: Map<Int, UniqueEmailCredential<*>>) :
        SharedPref<EmailTemplateRaw>(null) {
        val emailTemplateLabel by stringSharedPref("", index.toString())
        val emailTemplateSendTo by stringSharedPref("", index.toString())
        val emailTemplateCredentialId by intSharedPref(0, index.toString())

        override fun PrefWriterDslReceiver.write(value: EmailTemplateRaw?) {
            emailTemplateLabel.write(value?.label)
            emailTemplateSendTo.write(value?.sendTo)
            emailTemplateCredentialId.write(value?.uniqueCredential?.id)
        }

        override fun PrefReaderDslReceiver.read(): EmailTemplateRaw {
            val credentialId = emailTemplateCredentialId.read()
            return EmailTemplateRaw(
                emailTemplateLabel.read(),
                emailTemplateSendTo.read(),
                idToCredential[credentialId]
                    ?: error("Cannot find credential with id=$credentialId")
            )
        }
    }

    suspend fun sendEmail(activity: ComponentActivity, subject: String, body: String) {
        uniqueCredential.credential.sendEmail(activity, sendTo, subject, body)
    }
}
