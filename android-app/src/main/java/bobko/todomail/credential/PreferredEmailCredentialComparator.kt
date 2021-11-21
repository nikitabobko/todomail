package bobko.todomail.credential

import android.content.Context
import bobko.todomail.model.*
import bobko.todomail.util.readPref

class PreferredEmailCredentialComparator(val context: Context) : Comparator<EmailCredential> {
    override fun compare(x: EmailCredential, y: EmailCredential): Int {
        fun EmailCredential.index() = when (this) {
            is GoogleEmailCredential -> 0
            is SmtpCredential -> 1
        }
        compareBy<EmailCredential> { it.index() }.thenBy { it.label }.compare(x, y)
        return x.index().compareTo(y.index())
    }
}

fun suggestEmailTemplateLabel(context: Context): String {
    val existingLabels =
        context.readPref { EmailTemplate.All.read() }.mapTo(mutableSetOf()) { it.label }
    return sequenceOf("Todo", "Work")
        .plus(generateSequence(0) { it + 1 }.map { "Todo$it" })
        .first { it !in existingLabels }
}

fun suggestUniqueCredential(context: Context) =
    context.readPref { UniqueEmailCredential.All.read() }
        .sortedWith(compareBy(PreferredEmailCredentialComparator(context)) { it.credential })
        .firstOrNull()
        ?: UniqueEmailCredential.new(SmtpCredential.default, context)

fun suggestEmailTemplate(context: Context) =
    EmailTemplate.new(
        suggestEmailTemplateLabel(context),
        "",
        suggestUniqueCredential(context),
        context
    )