package bobko.todomail.settings.emailtemplate

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import bobko.todomail.R
import bobko.todomail.model.*
import bobko.todomail.util.*
import kotlin.reflect.KClass

sealed class TextFieldItem<T : Any, TEmailCredential : EmailCredential>(
    val label: String,
    val clazz: KClass<T>,
    val lens: Lens<EmailTemplate<TEmailCredential>, T>,
    val keyboardType: KeyboardType = KeyboardType.Text
) {
    var manuallyEditedAtLeastOnce: Boolean = false

    fun getCurrentText(emailTemplate: EmailTemplate<TEmailCredential>): String {
        return lens.get(emailTemplate).takeIf { it != -1 }?.toString() ?: ""
    }

    open fun getErrorIfAny(currentText: String): String? = null

    @Composable
    open fun Content(
        emailTemplate: MutableState<EmailTemplate<TEmailCredential>>,
        viewModel: EditEmailTemplateSettingsFragmentViewModel
    ) {
        CenteredRow {
            MyTextField(this@TextFieldItem, viewModel, emailTemplate)
        }
    }
}

class LabelTextFieldItem<TEmailCredential : EmailCredential>(context: Context) : TextFieldItem<String, TEmailCredential>(
    "Label",
    String::class,
    EmailTemplate<TEmailCredential>::label.lens,
) {
    val existingLabels = context.readPref { EmailTemplate.All.read() }
        .mapTo(mutableSetOf()) { it.label }

    override fun getErrorIfAny(currentText: String): String? {
        return if (currentText in existingLabels) "Label '$currentText' already exist" else null
    }
}

class SendToTextFieldItem<TEmailCredential : EmailCredential> : TextFieldItem<String, TEmailCredential>(
    "Send to",
    String::class,
    EmailTemplate<TEmailCredential>::sendTo.lens
)

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

    private fun getKnownSmtpServerPortPair(smtpCredential: SmtpCredential): KnownSmtpCredential? =
        KnownSmtpCredential.findBySmtpServer(smtpCredential)
            ?.takeIf { smtpCredential.smtpServerPort == it.smtpCredential.smtpServerPort }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content(
        emailTemplate: MutableState<EmailTemplate<SmtpCredential>>,
        viewModel: EditEmailTemplateSettingsFragmentViewModel
    ) {
        val knownSmtpServerPortPair = getKnownSmtpServerPortPair(emailTemplate.value.uniqueCredential.credential)
        CenteredRow {
            MyTextField(this@SmtpServerPortTextFieldItem, viewModel, emailTemplate)
            AnimatedVisibility(visible = knownSmtpServerPortPair != null) {
                CenteredRow {
                    Column {
                        // OutlinedTextField has small label at top which makes centering a bit offseted to the bottom
                        Spacer(modifier = Modifier.size(8.dp))
                        Icon(
                            painterResource(id = R.drawable.verified_icon_24),
                            "",
                            modifier = Modifier.size(emailIconSize),
                            tint = MaterialTheme.colors.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    }
}

class UsernameTextFieldItem : TextFieldItem<String, SmtpCredential>(
    "Username", // TODO rename to email?
    String::class,
    smtpCredentialLens.map { it::username }
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
        viewModel: EditEmailTemplateSettingsFragmentViewModel
    ) {
        Column {
            var textFieldVisualTransformation: VisualTransformation by remember {
                mutableStateOf(PasswordVisualTransformation())
            }
            CenteredRow {
                MyTextField(
                    this@PasswordTextFieldItem,
                    viewModel,
                    emailTemplate,
                    visualTransformation = textFieldVisualTransformation
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            CenteredRow(modifier = Modifier.padding(start = 16.dp)) {
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
    viewModel: EditEmailTemplateSettingsFragmentViewModel,
    emailTemplate: MutableState<EmailTemplate<TEmailCredential>>,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val currentText = item.getCurrentText(emailTemplate.value)
    val showErrorIfFieldIsEmpty by viewModel.showErrorIfFieldIsEmpty.observeAsState()
    val error = item.getErrorIfAny(currentText)
        ?: item.label.takeIf { showErrorIfFieldIsEmpty && currentText.isBlank() }
    OutlinedTextField(
        value = currentText,
        label = { Text(error ?: item.label) },
        isError = error != null,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp)
            .weight(1f)
            .focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(
            keyboardType = item.keyboardType,
//            imeAction = if (index == viewModel.schema.lastIndex) ImeAction.Done else ImeAction.Next TODO
        ),
        // TODO IDE hadn't completed this parameter :( Need to fix in Kotlin plugin
        visualTransformation = visualTransformation,
        keyboardActions = KeyboardActions(
            onNext = {
                // TODO implement
            },
            onDone = {
                keyboard?.hide()
            }
        ),
        onValueChange = { newValueRaw ->
            item.manuallyEditedAtLeastOnce = true
            val newValue = when (item.clazz) {
                // FYI https://issuetracker.google.com/issues/204522152
                Int::class -> if (newValueRaw.isEmpty()) -1 else newValueRaw.toIntOrNull()
                String::class -> newValueRaw
                else -> error("")
            } as? T
            newValue?.let {
                emailTemplate.value = item.lens.set(emailTemplate.value, it)
            }
            // TODO implement automatic port completion for known smtp servers
        },
    )
}
