package bobko.todomail.model

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import bobko.todomail.R

enum class KnownSmtpCredential(
    val label: String,
    @DrawableRes val iconResId: Int,
    val smtpCredential: SmtpCredential,
    val domain: String,
) : ComposeIconProvider {
    Gmail(
        "Gmail (SMTP)",
        R.drawable.ic_gmail_icon,
        SmtpCredential("smtp.gmail.com", 587, username = "", password = ""),
        domain = "gmail.com"
    ) {
        override fun suggestEmailSuffix(currentLabel: String): String {
            return "+${currentLabel.lowercase()}@gmail.com"
        }
    },
    Outlook(
        "Outlook (SMTP)",
        R.drawable.outlook_icon,
        SmtpCredential("smtp-mail.outlook.com", 587, username = "", password = ""),
        domain = "outlook.com",
    ),
    Yahoo(
        "Yahoo Mail (SMTP)",
        R.drawable.yahoo_mail,
        SmtpCredential("smtp.mail.yahoo.com", 465, username = "", password = ""),
        domain = "yahoo.com",
    );

    @Composable
    override fun Icon() {
        Icon(
            painterResource(id = iconResId),
            contentDescription = label,
            modifier = Modifier.size(emailIconSize),
            tint = Color.Unspecified
        )
    }

    open fun suggestEmailSuffix(currentLabel: String) = "@$domain"

    companion object {
        fun findBySmtpServer(smtpCredential: SmtpCredential) =
            values().singleOrNull { it.smtpCredential.smtpServer == smtpCredential.smtpServer }
    }
}

val emailIconSize = 32.dp
