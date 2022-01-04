/*
 * This file is part of Todomail.
 *
 * Todomail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Todomail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Todomail. If not, see <https://www.gnu.org/licenses/>.
 */

package bobko.todomail.credential

import android.content.Context
import bobko.todomail.credential.sealed.EmailCredential
import bobko.todomail.credential.sealed.GoogleEmailCredential
import bobko.todomail.credential.sealed.SmtpCredential
import bobko.todomail.model.EmailTemplate
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
