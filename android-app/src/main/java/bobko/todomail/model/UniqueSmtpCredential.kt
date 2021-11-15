package bobko.todomail.model

import android.content.Context
import bobko.todomail.pref.ListSharedPref
import bobko.todomail.pref.SharedPref
import bobko.todomail.pref.intSharedPref
import bobko.todomail.util.*

data class UniqueSmtpCredential private constructor(val id: Int, val credential: SmtpCredential) {
    companion object {
        fun new(smtpCredential: SmtpCredential, context: Context): UniqueSmtpCredential {
            val id = context.readPref {
                All.read().maxOf { it.id } + 1
            }
            return UniqueSmtpCredential(id, smtpCredential)
        }
    }

    object All : ListSharedPref<UniqueSmtpCredential>(
        null,
        SmtpCredential::class.simpleName!!,
        ::Pref
    )

    private class Pref(val index: Int) : SharedPref<UniqueSmtpCredential>(null) {
        private val credentialId by intSharedPref(0, index.toString())

        override fun PrefWriterDslReceiver.write(value: UniqueSmtpCredential?) {
            credentialId.write(value?.id)
            SmtpCredential.Pref(index).write(value?.credential)
        }

        override fun PrefReaderDslReceiver.read(): UniqueSmtpCredential {
            return UniqueSmtpCredential(
                credentialId.read(),
                SmtpCredential.Pref(index).read()
            )
        }
    }
}
