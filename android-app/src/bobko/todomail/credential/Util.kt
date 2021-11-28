package bobko.todomail.credential

import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

fun createEmail(
    from: String,
    to: String,
    subject: String,
    body: String,
    session: Session = Session.getDefaultInstance(Properties())
) = MimeMessage(session).apply {
    setFrom(from)
    setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
    this.subject = subject
    setText(body)
}
