package bobko.todomail.model

import androidx.activity.ComponentActivity
import bobko.todomail.pref.*
import bobko.todomail.util.*

typealias EmailTemplateRaw = EmailTemplate<*>

data class EmailTemplate<out T : EmailCredential>(
    val label: String,
    val sendTo: String,
    val uniqueCredential: UniqueEmailCredential<T>
) {
    object All : SharedPref<List<EmailTemplateRaw>>(null) {
        private val uniqueSuffix get() = EmailTemplate::class.simpleName!!

        override fun PrefWriterDslReceiver.writeImpl(value: List<EmailTemplateRaw>?) {
            UniqueEmailCredential.All.write(value?.map { it.uniqueCredential })
            ListSharedPref(null, uniqueSuffix) { Pref(it, mapOf()) }.write(value)
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

        override fun PrefWriterDslReceiver.writeImpl(value: EmailTemplateRaw?) {
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

    fun sendEmail(activity: ComponentActivity, subject: String, body: String) {
//        uniqueCredential.credential.sendEmail(activity, sendTo, subject, body)
    }
}
