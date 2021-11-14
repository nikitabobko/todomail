package bobko.todomail

import bobko.todomail.model.EmailTemplate
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailManager {
    fun sendEmailToMyself(emailTemplate: EmailTemplate, subject: String, body: String) {
        val prop = Properties()
        prop["mail.smtp.host"] = emailTemplate.uniqueCredential.credential.smtpServer
        prop["mail.smtp.port"] = emailTemplate.uniqueCredential.credential.smtpServerPort
        prop["mail.smtp.auth"] = "true"
        prop["mail.smtp.starttls.enable"] = "true"

        val session = Session.getInstance(prop, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(emailTemplate.uniqueCredential.credential.username, emailTemplate.uniqueCredential.credential.password)
            }
        })

        val message = MimeMessage(session).apply {
            setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(emailTemplate.sendTo)
            )
            this.subject = subject
            setText(body)
        }

        Transport.send(message)
    }
}
