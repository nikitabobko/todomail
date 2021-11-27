package bobko.todomail.model

import android.content.Context
import bobko.todomail.pref.ListSharedPref
import bobko.todomail.pref.SharedPref
import bobko.todomail.pref.intSharedPref
import bobko.todomail.util.*

data class UniqueEmailCredential<out T : EmailCredential> private constructor(
    val id: Int,
    val credential: T
) {
    companion object {
        // TODO add overflow logging
        val uniqueCredentialId by intSharedPref(0)

        fun <T : EmailCredential> new(emailCredential: T, context: Context, forceNew: Boolean = false): UniqueEmailCredential<T> {
            val existing: UniqueEmailCredential<T>? =
                if (!forceNew) {
                    context.readPref { All.read() }.firstOrNull { emailCredential == it.credential }
                        ?.cast<UniqueEmailCredential<T>>()
                } else null
            return existing ?: UniqueEmailCredential(
                context.writePref {
                    uniqueCredentialId.read().also { uniqueCredentialId.write(it + 1) }
                },
                emailCredential
            )
        }
    }

    object All : ListSharedPref<UniqueEmailCredential<*>>(
        null,
        UniqueEmailCredential::class.simpleName!!,
        ::Pref
    ) {
        override fun writeList(dslReceiver: PrefWriterDslReceiver, value: List<UniqueEmailCredential<*>>?) = with(dslReceiver) {
            require(value?.toSet()?.size == value?.size)
            value?.groupBy { it.id }?.forEach { check(it.value.toSet().size == 1) }
            value?.groupBy { it.credential }?.forEach { check(it.value.toSet().size == 1) }
            super.writeList(dslReceiver, value)
            uniqueCredentialId.write(value?.maxOfOrNull { it.id }?.plus(1) ?: 0)
        }

        override fun normalize(value: List<UniqueEmailCredential<*>>?) =
            value?.toSet()?.toList()
    }

    fun isDisposed(context: Context) = context.readPref { All.read() }.none { it.id == this.id }

    private class Pref(val index: Int) : SharedPref<UniqueEmailCredential<*>>(null) {
        private val credentialId by intSharedPref(0, index.toString())

        override fun PrefWriterDslReceiver.writeImpl(value: UniqueEmailCredential<*>?) {
            credentialId.write(value?.id)
            EmailCredential.Pref(index).write(value?.credential)
        }

        override fun PrefReaderDslReceiver.read() =
            UniqueEmailCredential(credentialId.read(), EmailCredential.Pref(index).read())
    }
}
