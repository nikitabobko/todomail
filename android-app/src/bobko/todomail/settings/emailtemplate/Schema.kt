package bobko.todomail.settings.emailtemplate

import android.accounts.AccountManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import bobko.todomail.R
import bobko.todomail.credential.SmtpCredentialType
import bobko.todomail.credential.emailIconSize
import bobko.todomail.credential.sealed.EmailCredential
import bobko.todomail.credential.sealed.SmtpCredential
import bobko.todomail.credential.sealed.type
import bobko.todomail.model.*
import bobko.todomail.util.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlin.reflect.KClass

sealed class TextFieldItem<T : Any, TEmailCredential : EmailCredential>(
    val label: String,
    val clazz: KClass<T>,
    val lens: Lens<EmailTemplate<TEmailCredential>, T>,
    val keyboardType: KeyboardType = KeyboardType.Text
) {
    val focusRequester = FocusRequester()

    fun getCurrentText(emailTemplate: EmailTemplate<TEmailCredential>): String {
        return lens.get(emailTemplate).takeIf { it != -1 }?.toString() ?: ""
    }

    open fun getErrorIfAny(currentText: String): String? = null

    @Composable
    open fun Content(
        emailTemplate: MutableState<EmailTemplate<TEmailCredential>>,
        fields: List<TextFieldItem<*, TEmailCredential>>,
        viewModel: EditEmailTemplateSettingsFragmentViewModel
    ) {
        CenteredRow(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
            MyTextField(this@TextFieldItem, fields, viewModel, emailTemplate)
        }
    }
}

class LabelTextFieldItem<TEmailCredential : EmailCredential>(
    currentEmailTemplate: EmailTemplate<TEmailCredential>,
    context: Context
) : TextFieldItem<String, TEmailCredential>(
    "Label",
    String::class,
    EmailTemplate<TEmailCredential>::label.lens,
) {
    private val existingLabels = context.readPref { EmailTemplate.All.read() }
        .filter { it.id != currentEmailTemplate.id }
        .mapTo(mutableSetOf()) { it.label }

    override fun getErrorIfAny(currentText: String): String? {
        return if (currentText in existingLabels) "Label '$currentText' already exist" else null
    }
}

class SendToTextFieldItem<TEmailCredential : EmailCredential> : TextFieldItem<String, TEmailCredential>(
    "Send to",
    String::class,
    EmailTemplate<TEmailCredential>::sendTo.lens,
    keyboardType = KeyboardType.Email,
) {
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content(
        emailTemplate: MutableState<EmailTemplate<TEmailCredential>>,
        fields: List<TextFieldItem<*, TEmailCredential>>,
        viewModel: EditEmailTemplateSettingsFragmentViewModel
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
            val interactionSource = remember { MutableInteractionSource() }
            CenteredRow {
                MyTextField(
                    this@SendToTextFieldItem,
                    fields,
                    viewModel,
                    emailTemplate,
                    interactionSource = interactionSource
                )
            }
            val deviceEmails = remember(emailTemplate.value.label) {
                AccountManager.get(viewModel.getApplication()).accounts.flatMap {
                    it.name.labelizeEmail(emailTemplate.value.label)
                }
            }
            val emails = deviceEmails
                .plus(emailTemplate.value.uniqueCredential.credential.email.labelizeEmail(emailTemplate.value.label))
                .filter { it.contains(emailTemplate.value.sendTo) && it != emailTemplate.value.sendTo }
                .distinct()
            var dismissed by remember(emailTemplate.value) { mutableStateOf(false) }
            LaunchedEffect(interactionSource) {
                interactionSource.interactions
                    .filter { it is PressInteraction.Press }
                    .collect { dismissed = false }
            }
            DropdownMenu(
                expanded = interactionSource.collectIsFocusedAsState().value && emails.isNotEmpty() && !dismissed,
                onDismissRequest = { dismissed = true },
                properties = PopupProperties(focusable = false) // Don't hide keyboard
            ) {
                emails.forEach { email ->
                    DropdownMenuItem(
                        onClick = { emailTemplate.value = emailTemplate.value.copy(sendTo = email) }
                    ) {
                        Text(email)
                    }
                }
            }
        }
    }

    private fun String.labelizeEmail(label: String) = split("@")
        .takeIf { it.size == 2 }
        ?.let { (local, domain) ->
            if (label.isBlank()) {
                listOf("$local@$domain")
            } else {
                listOf("$local@$domain", "$local+${label.lowercase()}@$domain")
            }
        }
        ?: listOf()
}

private val smtpCredentialLens
    get() = EmailTemplate<SmtpCredential>::uniqueCredential.map { it::credential }

class SmtpServerTextField : TextFieldItem<String, SmtpCredential>(
    "SMTP Server",
    String::class,
    smtpCredentialLens.map { it::smtpServer }
)

class SmtpServerPortTextFieldItem : TextFieldItem<Int, SmtpCredential>(
    "SMTP Server Port",
    Int::class,
    smtpCredentialLens.map { it::smtpServerPort },
    keyboardType = KeyboardType.Number
) {
    override fun getErrorIfAny(currentText: String): String? {
        val port = currentText.toIntOrNull() ?: 0
        return when {
            port < 0 -> "SMTP Server Port cannot be negative"
            port > UShort.MAX_VALUE.toInt() -> "SMTP Server Port max possible value is ${UShort.MAX_VALUE.toInt()}"
            else -> null
        }
    }

    private fun getKnownSmtpServerPortPair(smtpCredential: SmtpCredential): SmtpCredentialType? =
        SmtpCredentialType.findBySmtpServer(smtpCredential)
            .takeIf { smtpCredential.smtpServerPort == it.smtpCredential.smtpServerPort }
            .takeIf { it != SmtpCredentialType.Generic }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content(
        emailTemplate: MutableState<EmailTemplate<SmtpCredential>>,
        fields: List<TextFieldItem<*, SmtpCredential>>,
        viewModel: EditEmailTemplateSettingsFragmentViewModel
    ) {
        val knownSmtpServerPortPair = getKnownSmtpServerPortPair(emailTemplate.value.uniqueCredential.credential)
        CenteredRow(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
            MyTextField(this@SmtpServerPortTextFieldItem, fields, viewModel, emailTemplate)
            AnimatedVisibility(visible = knownSmtpServerPortPair != null) {
                CenteredRow {
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Spacer(modifier = Modifier.size(OutlinedTextFieldTopPadding))
                        Icon(
                            painterResource(id = R.drawable.verified_icon_24),
                            "",
                            modifier = Modifier.size(emailIconSize),
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}

class UsernameTextFieldItem : TextFieldItem<String, SmtpCredential>(
    "Username",
    String::class,
    smtpCredentialLens.map { it::username },
    keyboardType = KeyboardType.Email,
)

class PasswordTextFieldItem : TextFieldItem<String, SmtpCredential>(
    "Password",
    String::class,
    smtpCredentialLens.map { it::password },
    keyboardType = KeyboardType.Password
) {
    @Composable
    override fun Content(
        emailTemplate: MutableState<EmailTemplate<SmtpCredential>>,
        fields: List<TextFieldItem<*, SmtpCredential>>,
        viewModel: EditEmailTemplateSettingsFragmentViewModel
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp)) {
            var textFieldVisualTransformation: VisualTransformation by remember {
                mutableStateOf(PasswordVisualTransformation())
            }
            when (emailTemplate.value.uniqueCredential.credential.type) {
                SmtpCredentialType.Gmail, SmtpCredentialType.Yahoo -> {
                    CenteredRow {
                        Icon(Icons.Rounded.Warning, contentDescription = "")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Make sure to use \"App password\" instead of regular password", style = MaterialTheme.typography.caption)
                    }
                }
                SmtpCredentialType.Outlook, SmtpCredentialType.Generic -> {}
            }
            CenteredRow {
                MyTextField(
                    this@PasswordTextFieldItem,
                    fields,
                    viewModel,
                    emailTemplate,
                    visualTransformation = textFieldVisualTransformation
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            CenteredRow {
                val onClick = {
                    textFieldVisualTransformation =
                        if (textFieldVisualTransformation is PasswordVisualTransformation) VisualTransformation.None
                        else PasswordVisualTransformation()
                }
                Checkbox(
                    checked = textFieldVisualTransformation !is PasswordVisualTransformation,
                    onCheckedChange = { onClick() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Show password",
                    modifier = Modifier.clickable(onClick = onClick)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
private fun <T : Any, TEmailCredential : EmailCredential> RowScope.MyTextField(
    item: TextFieldItem<T, TEmailCredential>,
    fields: List<TextFieldItem<*, TEmailCredential>>,
    viewModel: EditEmailTemplateSettingsFragmentViewModel,
    emailTemplate: MutableState<EmailTemplate<TEmailCredential>>,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val index = fields.indexOf(item).also { check(it != -1) }

    val currentText = item.getCurrentText(emailTemplate.value)
    val showErrorIfFieldIsEmpty by viewModel.showErrorIfFieldIsEmpty.observeAsState()
    val error = item.getErrorIfAny(currentText)
        ?: item.label.takeIf { showErrorIfFieldIsEmpty && currentText.isBlank() }
    OutlinedTextField(
        value = currentText,
        label = { Text(error ?: item.label) },
        isError = error != null,
        modifier = Modifier
            .weight(1f)
            .focusRequester(item.focusRequester),
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(
            keyboardType = item.keyboardType,
            imeAction = if (index == fields.lastIndex) ImeAction.Done else ImeAction.Next
        ),
        // TODO IDE hadn't completed this parameter :( Need to fix in Kotlin plugin
        visualTransformation = visualTransformation,
        keyboardActions = KeyboardActions(
            onNext = { fields.getOrNull(index + 1)?.focusRequester?.requestFocus() },
            onDone = { keyboard?.hide() }
        ),
        onValueChange = { newValueRaw ->
            val newValue = when (item.clazz) {
                // FYI https://issuetracker.google.com/issues/204522152
                Int::class -> if (newValueRaw.isEmpty()) -1 else newValueRaw.toIntOrNull()
                String::class -> newValueRaw
                else -> error("")
            } as? T
            newValue?.let {
                emailTemplate.value = item.lens.set(emailTemplate.value, it)
            }
        },
    )
}
