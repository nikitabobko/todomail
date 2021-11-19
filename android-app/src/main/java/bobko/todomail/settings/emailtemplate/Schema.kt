//package bobko.todomail.settings.emailtemplate
//
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.focus.FocusRequester
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.text.input.PasswordVisualTransformation
//import androidx.compose.ui.text.input.VisualTransformation
//import androidx.compose.ui.unit.dp
//import bobko.todomail.R
//import bobko.todomail.model.EmailTemplateRaw
//import bobko.todomail.settings.KnownSmtpCredential
//import bobko.todomail.settings.emailIconSize
//import bobko.todomail.util.*
//import kotlin.reflect.KClass
//
//fun getSchema(existingLabels: Set<String>) = listOf(
//    LabelTextFieldItem(existingLabels),
//
//    TextDivider("Credentials settings"),
//    SmtpServerTextFieldItem(),
//    SmtpServerPortTextFieldItem(),
//    UsernameTextFieldItem(),
//    PasswordTextFieldItem(),
//
//    TextDivider("Destination address settings"),
//    SendToTextFieldItem(),
//)
//
//sealed class Item {
//    @Composable
//    abstract fun Composable(
//        emailTemplate: MutableState<EmailTemplateRaw>,
//        viewModel: EditEmailTemplateSettingsFragmentViewModel
//    )
//}
//
//class TextDivider(val text: String) : Item() {
//    @Composable
//    override fun Composable(
//        emailTemplate: MutableState<EmailTemplateRaw>,
//        viewModel: EditEmailTemplateSettingsFragmentViewModel
//    ) {
//        Spacer(modifier = Modifier.height(8.dp))
//        bobko.todomail.settings.TextDivider(text = text)
//    }
//}
//
//
//
//sealed class TextFieldItem<T : Any>(
//    val label: String,
//    val lens: Lens<EmailTemplateRaw, T>,
//    val clazz: KClass<T>,
//    val keyboardType: KeyboardType = KeyboardType.Text,
//    var focusRequester: FocusRequester? = null,
//    var wasTextChangedManuallyAtLeastOnce: Boolean = false,
//) : Item() {
//    init {
//        require(clazz == Int::class || clazz == String::class)
//    }
//
//    fun getCurrentText(emailTemplate: EmailTemplateRaw): String {
//        return lens.get(emailTemplate).takeIf { it != -1 }?.toString() ?: ""
//    }
//
//    open fun errorProvider(currentText: String): String? = null
//    open fun onTextChanged(emailTemplate: MutableState<EmailTemplateRaw>) {}
//    open fun isRightSideHintVisible(emailTemplate: EmailTemplateRaw) = false
//
//    @Composable
//    open fun RightSideHint(emailTemplate: EmailTemplateRaw) {
//    }
//
//    open val textFieldVisualTransformation: State<VisualTransformation> =
//        mutableStateOf(VisualTransformation.None)
//
//    @Composable
//    override fun Composable(
//        emailTemplate: MutableState<EmailTemplateRaw>,
//        viewModel: EditEmailTemplateSettingsFragmentViewModel
//    ) {
//        TextFieldComposable(emailTemplate, viewModel)
//    }
//}
//
//class LabelTextFieldItem(private val existingLabels: Set<String>) : TextFieldItem<String>(
//    "Label",
//    EmailTemplateRaw::label.lens,
//    String::class
//) {
//    override fun errorProvider(currentText: String) =
//        if (currentText in existingLabels) "Label '$currentText' already exist" else null
//}
//
//class SmtpServerTextFieldItem() : TextFieldItem<String>(
//    "SMTP Server",
//    EmailTemplateRaw::uniqueCredential.map { ::credential }.map { ::smtpServer },
//    String::class,
//) {
//    override fun onTextChanged(emailTemplate: MutableState<EmailTemplateRaw>) {
//        val smtpServerPortLens = EmailTemplateRaw::uniqueCredential.map { ::credential }.map { ::smtpServerPort }
//        if (smtpServerPortLens.get(emailTemplate.value) == DEFAULT_SMTP_PORT) {
//            KnownSmtpCredential.findBySmtpServer(emailTemplate.value)
//                ?.smtpCredential
//                ?.smtpServerPort
//                ?.let {
//                    emailTemplate.value = smtpServerPortLens.set(emailTemplate.value, it)
//                }
//        }
//    }
//
//    override fun isRightSideHintVisible(emailTemplate: EmailTemplateRaw) =
//        KnownSmtpCredential.findBySmtpServer(emailTemplate) != null
//
//    // TODO report that Kotlin plugin doesn't complete @Composable method override
//    @Composable
//    override fun RightSideHint(emailTemplate: EmailTemplateRaw) {
//        KnownSmtpCredential.findBySmtpServer(emailTemplate)?.Icon()
//    }
//}
//
//class SmtpServerPortTextFieldItem() : TextFieldItem<Int>(
//    "SMTP Server Port",
//    EmailTemplateRaw::uniqueCredential.map { ::credential }.map { ::smtpServerPort },
//    Int::class,
//    KeyboardType.Number,
//) {
//    override fun errorProvider(currentText: String): String? {
//        val port = currentText.toIntOrNull() ?: 0
//        return when {
//            port < 0 -> "SMTP Server Port cannot be negative"
//            port > UShort.MAX_VALUE.toInt() -> "SMTP Server Port max possible value is ${UShort.MAX_VALUE.toInt()}"
//            else -> null
//        }
//    }
//
//    private fun getKnownSmtpServerPortPair(emailTemplate: EmailTemplateRaw): KnownSmtpCredential? =
//        KnownSmtpCredential.findBySmtpServer(emailTemplate)
//            ?.takeIf { emailTemplate.uniqueCredential.credential.smtpServerPort == it.smtpCredential.smtpServerPort }
//
//    override fun isRightSideHintVisible(emailTemplate: EmailTemplateRaw) =
//        getKnownSmtpServerPortPair(emailTemplate) != null
//
//    @Composable
//    override fun RightSideHint(emailTemplate: EmailTemplateRaw) {
//        getKnownSmtpServerPortPair(emailTemplate)?.let {
//            Icon(
//                painterResource(id = R.drawable.verified_icon_24),
//                "",
//                modifier = Modifier.size(emailIconSize),
//                tint = MaterialTheme.colors.primary
//            )
//        }
//    }
//}
//
//class PasswordTextFieldItem() : TextFieldItem<String>(
//    "Password",
//    EmailTemplateRaw::uniqueCredential.map { ::credential }.map { ::password },
//    String::class,
//    KeyboardType.Password,
//) {
//    override val textFieldVisualTransformation: MutableState<VisualTransformation> =
//        mutableStateOf(PasswordVisualTransformation())
//
//    @Composable
//    override fun Composable(
//        emailTemplate: MutableState<EmailTemplateRaw>,
//        viewModel: EditEmailTemplateSettingsFragmentViewModel
//    ) {
//        Column {
//            TextFieldComposable(emailTemplate, viewModel)
//            Spacer(modifier = Modifier.height(8.dp))
//            val onClick = {
//                textFieldVisualTransformation.value =
//                    if (textFieldVisualTransformation.value is PasswordVisualTransformation) VisualTransformation.None
//                    else PasswordVisualTransformation()
//            }
//            CenteredRow(modifier = Modifier.padding(start = 16.dp)) {
//                Checkbox(
//                    checked = textFieldVisualTransformation.value !is PasswordVisualTransformation,
//                    onCheckedChange = { onClick() }
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    "Show password",
//                    modifier = Modifier.clickable(onClick = onClick)
//                )
//            }
//        }
//    }
//}
//
//class UsernameTextFieldItem() : TextFieldItem<String>(
//    "Username",
//    EmailTemplateRaw::uniqueCredential.map { ::credential }.map { ::username },
//    String::class,
//    KeyboardType.Email
//)
//
//class SendToTextFieldItem() : TextFieldItem<String>(
//    "Send to",
//    EmailTemplateRaw::sendTo.lens,
//    String::class,
//    KeyboardType.Email,
//) {
//    override fun isRightSideHintVisible(emailTemplate: EmailTemplateRaw) =
//        emailTemplate.sendTo.run { isNotBlank() && !contains("@") } &&
//                KnownSmtpCredential.findBySmtpServer(emailTemplate) != null
//
//    @Composable
//    override fun RightSideHint(emailTemplate: EmailTemplateRaw) {
//        KnownSmtpCredential.findBySmtpServer(emailTemplate)
//            ?.suggestEmailSuffix(emailTemplate.label)
//            ?.let { suggestedEmailSuffix ->
//                OutlinedButton(onClick = { /*TODO*/ }, modifier = Modifier.height(56.dp)) {
//                    Text(suggestedEmailSuffix)
//                }
//            }
//    }
//}
