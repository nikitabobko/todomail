package bobko.todomail.model

import bobko.todomail.util.*

data class SmtpCredential(
    val smtpServer: String,
    val smtpServerPort: Int,
    val username: String,
    val password: String,
) {
    class Pref(index: Int) : SharedPref<SmtpCredential>(null) {
        private val smtpServer by stringSharedPref("", index.toString())
        private val smtpServerPort by intSharedPref(0, index.toString())
        private val smtpUsername by stringSharedPref("", index.toString())
        private val smtpPassword by stringSharedPref("", index.toString())

        override fun PrefWriterDslReceiver.write(value: SmtpCredential?) {
            smtpServer.write(value?.smtpServer)
            smtpServerPort.write(value?.smtpServerPort)
            smtpUsername.write(value?.username)
            smtpPassword.write(value?.password)
        }

        override fun PrefReaderDslReceiver.read() = SmtpCredential(
            smtpServer.read(),
            smtpServerPort.read(),
            smtpUsername.read(),
            smtpPassword.read(),
        )
    }
}
