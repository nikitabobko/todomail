package bobko.todomail.model

import android.content.Context
import bobko.todomail.pref.ListSharedPref
import bobko.todomail.pref.SharedPref
import bobko.todomail.pref.intSharedPref
import bobko.todomail.util.PrefReaderDslReceiver
import bobko.todomail.util.PrefWriterDslReceiver
import bobko.todomail.util.writePref

data class UniqueEmailCredential<out T : EmailCredential> private constructor(
    val id: Int,
    val credential: T
) {
    companion object {
        val uniqueCredentialId by intSharedPref(0)

        fun <T : EmailCredential> new(emailCredential: T, context: Context): UniqueEmailCredential<T> {
            val id = context.writePref {
                uniqueCredentialId.read().also { uniqueCredentialId.write(it + 1) }
            }
            return UniqueEmailCredential(id, emailCredential)
        }
    }

    object All : ListSharedPref<UniqueEmailCredential<*>>(
        null,
        UniqueEmailCredential::class.simpleName!!,
        ::Pref
    ) {
        override fun writeList(dslReceiver: PrefWriterDslReceiver, value: List<UniqueEmailCredential<*>>?) = with(dslReceiver) {
            value?.groupBy { it.id }
                ?.asSequence()
                ?.map { (_, uniqueCredentialsWithSameId) -> uniqueCredentialsWithSameId.map { it.credential } }
                ?.forEach { check(it.toSet().size == 1) }
            super.writeList(dslReceiver, value)
            uniqueCredentialId.write(value?.maxOfOrNull { it.id }?.plus(1) ?: 0)
        }
    }

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

suspend fun PrefWriterDslReceiver.garbageCollectUnreachableCredentials(context: Context) {
    val reachableIds = EmailTemplate.All.read().mapTo(mutableSetOf()) { it.uniqueCredential.id }
    val (reachable, unreachable) = UniqueEmailCredential.All.read().partition { it.id in reachableIds }
    unreachable.forEach { it.credential.signOut(context) }
    UniqueEmailCredential.All.write(reachable)
}
