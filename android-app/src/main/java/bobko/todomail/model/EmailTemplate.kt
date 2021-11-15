package bobko.todomail.model

import bobko.todomail.pref.ListSharedPref
import bobko.todomail.pref.SharedPref
import bobko.todomail.pref.intSharedPref
import bobko.todomail.pref.stringSharedPref
import bobko.todomail.util.*

data class EmailTemplate(
    val label: String,
    val sendTo: String,
    val uniqueCredential: UniqueSmtpCredential
) {
    object All : SharedPref<List<EmailTemplate>>(null) {
        private val uniqueSuffix get() = EmailTemplate::class.simpleName!!

        override fun PrefWriterDslReceiver.write(value: List<EmailTemplate>?) {
            val idToCredential =
                value?.associate { it.uniqueCredential.id to it.uniqueCredential } ?: emptyMap()
            UniqueSmtpCredential.All.write(value?.map { it.uniqueCredential })
            ListSharedPref(null, uniqueSuffix) { Pref(it, idToCredential) }.write(value)
        }

        override fun PrefReaderDslReceiver.read(): List<EmailTemplate> {
            val idToCredential =
                UniqueSmtpCredential.All.read().associateBy { it.id }
            return ListSharedPref(null, uniqueSuffix) { Pref(it, idToCredential) }.read()
        }
    }

    private class Pref(index: Int, val idToCredential: Map<Int, UniqueSmtpCredential>) :
        SharedPref<EmailTemplate>(null) {
        val emailTemplateLabel by stringSharedPref("", index.toString())
        val emailTemplateSendTo by stringSharedPref("", index.toString())
        val emailTemplateCredentialId by intSharedPref(0, index.toString())

        override fun PrefWriterDslReceiver.write(value: EmailTemplate?) {
            emailTemplateLabel.write(value?.label)
            emailTemplateSendTo.write(value?.sendTo)
            emailTemplateCredentialId.write(value?.uniqueCredential?.id)
        }

        override fun PrefReaderDslReceiver.read(): EmailTemplate {
            val credentialId = emailTemplateCredentialId.read()
            return EmailTemplate(
                emailTemplateLabel.read(),
                emailTemplateSendTo.read(),
                idToCredential[credentialId]
                    ?: error("Cannot find credential with id=$credentialId")
            )
        }
    }
}
