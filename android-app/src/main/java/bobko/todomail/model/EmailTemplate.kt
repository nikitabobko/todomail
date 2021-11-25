package bobko.todomail.model

import android.content.Context
import bobko.todomail.pref.ListSharedPref
import bobko.todomail.pref.SharedPref
import bobko.todomail.pref.intSharedPref
import bobko.todomail.pref.stringSharedPref
import bobko.todomail.util.PrefReaderDslReceiver
import bobko.todomail.util.PrefWriterDslReceiver
import bobko.todomail.util.writePref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias EmailTemplateRaw = EmailTemplate<*>

data class EmailTemplate<out T : EmailCredential> private constructor(
    val id: Int,
    val label: String,
    val sendTo: String,
    val uniqueCredential: UniqueEmailCredential<T>
) {
    companion object {
        // TODO add overflow logging
        val uniqueEmailTemplateId by intSharedPref(0)

        fun <T : EmailCredential> new(
            label: String,
            sendTo: String,
            uniqueEmailCredential: UniqueEmailCredential<T>,
            context: Context
        ): EmailTemplate<T> {
            val id = context.writePref {
                uniqueEmailTemplateId.read().also { uniqueEmailTemplateId.write(it + 1) }
            }
            return EmailTemplate(id, label, sendTo, uniqueEmailCredential)
        }
    }

    object All : SharedPref<List<EmailTemplateRaw>>(null) {
        private val uniqueSuffix get() = EmailTemplate::class.simpleName!!

        override fun PrefWriterDslReceiver.writeImpl(value: List<EmailTemplateRaw>?) {
            value?.groupBy { it.id }?.forEach { check(it.value.toSet().size == 1) }
            UniqueEmailCredential.All.write(value?.map { it.uniqueCredential })
            ListSharedPref(null, uniqueSuffix) { Pref(it, mapOf()) }.write(value)
            uniqueEmailTemplateId.write(value?.maxOfOrNull { it.id }?.plus(1) ?: 0)
        }

        override fun PrefReaderDslReceiver.read(): List<EmailTemplateRaw> {
            val idToCredential =
                UniqueEmailCredential.All.read().associateBy { it.id }
            return ListSharedPref(null, uniqueSuffix) { Pref(it, idToCredential) }.read()
        }
    }

    private class Pref(index: Int, val idToCredential: Map<Int, UniqueEmailCredential<*>>) :
        SharedPref<EmailTemplateRaw>(null) {
        val emailTemplateId by intSharedPref(0, index.toString())
        val emailTemplateLabel by stringSharedPref("", index.toString())
        val emailTemplateSendTo by stringSharedPref("", index.toString())
        val emailTemplateCredentialId by intSharedPref(0, index.toString())

        override fun PrefWriterDslReceiver.writeImpl(value: EmailTemplateRaw?) {
            emailTemplateId.write(value?.id)
            emailTemplateLabel.write(value?.label)
            emailTemplateSendTo.write(value?.sendTo)
            emailTemplateCredentialId.write(value?.uniqueCredential?.id)
        }

        override fun PrefReaderDslReceiver.read(): EmailTemplateRaw {
            val credentialId = emailTemplateCredentialId.read()
            return EmailTemplateRaw(
                emailTemplateId.read(),
                emailTemplateLabel.read(),
                emailTemplateSendTo.read(),
                idToCredential[credentialId]
                    ?: error("Cannot find credential with id=$credentialId")
            )
        }
    }

    fun <T : EmailCredential> switchCredential(uniqueCredential: UniqueEmailCredential<T>, context: Context) =
        new(
            label,
            sendTo,
            uniqueCredential,
            context
        )

    suspend fun sendEmail(context: Context, subject: String, body: String) {
        withContext(Dispatchers.IO) {
            uniqueCredential.credential.sendEmail(context, sendTo, subject, body)
        }
    }
}
