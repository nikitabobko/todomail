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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import bobko.todomail.R
import bobko.todomail.credential.sealed.EmailCredential
import bobko.todomail.credential.sealed.GoogleEmailCredential
import bobko.todomail.credential.sealed.SmtpCredential

sealed interface EmailCredentialType<TEmailCredential : EmailCredential> {
    @Composable
    fun Icon()
}

object GoogleCredentialType : EmailCredentialType<GoogleEmailCredential> {
    @Composable
    override fun Icon() {
        Icon(
            painterResource(R.drawable.google_logo),
            "Google logo",
            modifier = Modifier.size(emailIconSize),
            tint = Color.Unspecified
        )
    }
}

enum class SmtpCredentialType(
    val label: String,
    @DrawableRes val iconResId: Int,
    val smtpCredential: SmtpCredential,
    val domain: String?,
) : EmailCredentialType<SmtpCredential> {
    Gmail(
        "Gmail (SMTP)",
        R.drawable.ic_gmail_icon,
        SmtpCredential("smtp.gmail.com", 587, username = "", password = ""),
        domain = "gmail.com"
    ),
    Outlook(
        "Outlook (SMTP)",
        R.drawable.outlook_icon,
        SmtpCredential("smtp-mail.outlook.com", 587, username = "", password = ""),
        domain = "outlook.com",
    ),
    Yahoo(
        "Yahoo Mail (SMTP)",
        R.drawable.yahoo_mail,
        SmtpCredential("smtp.mail.yahoo.com", 587, username = "", password = ""),
        domain = "yahoo.com",
    ),
    Generic(
        "Email (SMTP)",
        R.drawable.email_icon_24,
        SmtpCredential.default,
        domain = null
    );

    @Composable
    override fun Icon() {
        Icon(
            painterResource(id = iconResId),
            contentDescription = label,
            modifier = Modifier.size(emailIconSize),
            tint = if (this == Generic) LocalContentColor.current else Color.Unspecified
        )
    }

    companion object {
        fun findBySmtpServer(smtpCredential: SmtpCredential) =
            values().singleOrNull { it.smtpCredential.smtpServer == smtpCredential.smtpServer } ?: Generic
    }
}

val emailIconSize = 32.dp
