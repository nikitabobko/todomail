package bobko.todomail.model

import android.content.Context
import bobko.todomail.pref.ListSharedPref
import bobko.todomail.pref.SharedPref
import bobko.todomail.pref.intSharedPref
import bobko.todomail.util.PrefReaderDslReceiver
import bobko.todomail.util.PrefWriterDslReceiver
import bobko.todomail.util.readPref

data class UniqueEmailCredential<out T : EmailCredential> private constructor(
    val id: Int,
    val credential: T
) {
    companion object {
        fun new(
            smtpCredential: SmtpCredential,
            context: Context
        ): UniqueEmailCredential<SmtpCredential> {
            val id = context.readPref {
                All.read().maxOfOrNull { it.id }?.plus(1) ?: 0
            }
            return UniqueEmailCredential(id, smtpCredential)
        }
    }

    object All : ListSharedPref<UniqueEmailCredential<*>>(
        null,
        UniqueEmailCredential::class.simpleName!!,
        ::Pref
    )

    private class Pref(val index: Int) : SharedPref<UniqueEmailCredential<*>>(null) {
        private val credentialId by intSharedPref(0, index.toString())

        override fun PrefWriterDslReceiver.write(value: UniqueEmailCredential<*>?) {
            credentialId.write(value?.id)
            EmailCredential.Pref(index).write(value?.credential)
        }

        override fun PrefReaderDslReceiver.read() =
            UniqueEmailCredential(credentialId.read(), EmailCredential.Pref(index).read())
    }
}
