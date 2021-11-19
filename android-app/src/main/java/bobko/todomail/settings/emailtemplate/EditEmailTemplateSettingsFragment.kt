package bobko.todomail.settings.emailtemplate

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import bobko.todomail.model.*
import bobko.todomail.settings.SettingsActivity
import bobko.todomail.settings.SettingsScreen
import bobko.todomail.settings.TextDivider
import bobko.todomail.util.*
import kotlin.reflect.KClass

const val DEFAULT_SMTP_PORT = 25

class EditEmailTemplateSettingsFragment : Fragment() {
    val viewModel by viewModels<EditEmailTemplateSettingsFragmentViewModel>()
    fun parentActivity() = requireActivity() as SettingsActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = requireContext().composeView {
        val existingTemplates = requireContext().readPref { EmailTemplate.All.read() }
        val mode =
            if (parentActivity().viewModel.emailTemplateToEdit in existingTemplates) Mode.Edit
            else Mode.Add
        val emailTemplate: MutableState<EmailTemplateRaw> =
            viewModel.emailTemplate.observeAsMutableState { // TODO add screen rotation test
                parentActivity().viewModel.emailTemplateToEdit ?: EmailTemplateRaw(
                    suggestEmailTemplateLabel(requireContext()), "",
                    UniqueEmailCredential.new(
                        SmtpCredential("", DEFAULT_SMTP_PORT, "", ""),
                        requireContext()
                    )
                )
            }
        EditEmailTemplateSettingsFragmentScreen(emailTemplate, mode)
    }
}


private val smtpFields: List<TextFieldState<*, SmtpCredential>>
    get() {
        val smtpCredentialLens =
            EmailTemplate<SmtpCredential>::uniqueCredential.map { it::credential }
        return listOf(
            TextFieldState(
                "SMTP Server",
                String::class,
                smtpCredentialLens.map { it::smtpServer }
            ),
            TextFieldState(
                "SMTP Server Port",
                Int::class,
                smtpCredentialLens.map { it::smtpServerPort }
            ),
            TextFieldState(
                "Username", // TODO rename to email?
                String::class,
                smtpCredentialLens.map { it::username }
            ),
            TextFieldState(
                "Password",
                String::class,
                smtpCredentialLens.map { it::password }
            ),
        )
    }

@Composable
private fun <TEmailCredential : EmailCredential> EditEmailTemplateSettingsFragment.getLabel(): TextFieldState<String, TEmailCredential> {
    val existingLabels = remember {
        requireContext().readPref { EmailTemplate.All.read() }
            .mapTo(mutableSetOf()) { it.label }
    }
    return TextFieldState(
        "Label",
        String::class,
        EmailTemplate<TEmailCredential>::label.lens,
        errorProvider = { currentText ->
            if (currentText in existingLabels) "Label '$currentText' already exist" else null
        }
    )
}

private fun <TEmailCredential : EmailCredential> getDestinationAddress(): TextFieldState<String, TEmailCredential> =
    TextFieldState(
        "Send to",
        String::class,
        EmailTemplate<TEmailCredential>::sendTo.lens
    )

//@OptIn(ExperimentalAnimationApi::class)
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun <TEmailCredential : EmailCredential> EditEmailTemplateSettingsFragment.EditEmailTemplateSettingsFragmentScreen(
    emailTemplate: MutableState<EmailTemplate<TEmailCredential>>,
    mode: Mode
) {
    SettingsScreen("Edit Email Template Settings") {
        Spacer(modifier = Modifier.height(16.dp))
        val label = getLabel<TEmailCredential>()
        MyTextField(label, emailTemplate)
        MyTextDivider("Credentials settings")
//        AnimatedContent(targetState = emailTemplate.value.uniqueCredential.credential) { credential ->
//            when (credential) {
//                GoogleEmailCredential -> {
//                    listOf()
//                }
//                is SmtpCredential -> {
//                    emailTemplate as MutableState<EmailTemplate<SmtpCredential>>
//                    smtpFields.forEach { MyTextField(it, emailTemplate) }
//                    smtpFields as List<TextFieldState<*, TEmailCredential>>
//                }
//                else -> error("Unknown EmailCredential type")
//            }
//        }
        ListItem {

        }
        val fields: List<TextFieldState<*, TEmailCredential>> =
            when (emailTemplate.value.uniqueCredential.credential) {
                GoogleEmailCredential -> {
                    listOf()
                }
                is SmtpCredential -> {
                    emailTemplate as MutableState<EmailTemplate<SmtpCredential>>
                    smtpFields.forEach { MyTextField(it, emailTemplate) }
                    smtpFields as List<TextFieldState<*, TEmailCredential>>
                }
                else -> error("Unknown EmailCredential type")
            }
        MyTextDivider("Destination address settings")
        val destinationAddress = getDestinationAddress<TEmailCredential>()
        MyTextField(destinationAddress, emailTemplate)
        Buttons(listOf(label) + fields, emailTemplate, mode)
    }
}

@Composable
private fun MyTextDivider(text: String) {
    Spacer(modifier = Modifier.height(8.dp))
    TextDivider(text)
}

private data class TextFieldState<T : Any, TEmailCredential : EmailCredential>(
    val label: String,
    val clazz: KClass<T>,
    val lens: Lens<EmailTemplate<TEmailCredential>, T>,
    val errorProvider: (currentText: String) -> String? = { null },
) {
    fun getCurrentText(emailTemplate: EmailTemplate<TEmailCredential>): String {
        return lens.get(emailTemplate).takeIf { it != -1 }?.toString() ?: ""
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
private fun <T : Any, TEmailCredential : EmailCredential> EditEmailTemplateSettingsFragment.MyTextField(
    state: TextFieldState<T, TEmailCredential>,
    emailTemplate: MutableState<EmailTemplate<TEmailCredential>>,
    errorProvider: (currentText: String) -> String? = { null },
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val currentText = state.getCurrentText(emailTemplate.value)
    val showErrorIfFieldIsEmpty by viewModel.showErrorIfFieldIsEmpty.observeAsState()
    val error = errorProvider(currentText)
        ?: state.label.takeIf { showErrorIfFieldIsEmpty && currentText.isBlank() }
    OutlinedTextField(
        value = currentText,
        label = { Text(error ?: state.label) },
        isError = error != null,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp)
            .fillMaxWidth()
            .focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
//            imeAction = if (index == viewModel.schema.lastIndex) ImeAction.Done else ImeAction.Next TODO
        ),
        // TODO IDE hadn't completed this parameter :( Need to fix in Kotlin plugin
//        visualTransformation = textFieldVisualTransformation.value,  TODO
        keyboardActions = KeyboardActions(
            onNext = {
                // TODO implement
            },
            onDone = {
                keyboard?.hide()
            }
        ),
        onValueChange = { newValueRaw ->
            val newValue = when (state.clazz) {
                // FYI https://issuetracker.google.com/issues/204522152
                Int::class -> if (newValueRaw.isEmpty()) -1 else newValueRaw.toIntOrNull()
                String::class -> newValueRaw
                else -> error("")
            } as? T?
            newValue?.let {
                emailTemplate.value = state.lens.set(emailTemplate.value, it)
            }
            // TODO implement automatic port completion for known smtp servers
        },
    )
}

@Composable
private fun <TEmailCredential : EmailCredential> EditEmailTemplateSettingsFragment.Buttons(
    textFields: List<TextFieldState<*, TEmailCredential>>,
    emailTemplate: MutableState<EmailTemplate<TEmailCredential>>,
    mode: Mode,
) {
    Spacer(modifier = Modifier.height(16.dp))
    CenteredRow(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
        if (mode == Mode.Edit) {
            OutlinedButton(
                onClick = {
                    findNavController().navigateUp() // TODO
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                CenteredRow {
                    Icon(Icons.Rounded.Delete, "", tint = Color.Red)
                    Text("Delete", color = Color.Red)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Button(
            modifier = Modifier.weight(1f),
            onClick = {
                if (textFields.any { field ->
                        field.getCurrentText(emailTemplate.value)
                            .let { it.isBlank() || field.errorProvider(it) != null }
                    }
                ) {
                    viewModel.showErrorIfFieldIsEmpty.value = true
                } else {
                    requireContext().writePref {
                        EmailTemplate.All.write(EmailTemplate.All.read() + emailTemplate.value)
                    }
                    findNavController().navigateUp()
                }
            }
        ) {
            CenteredRow {
                when (mode) {
                    Mode.Edit -> {
                        Icon(Icons.Rounded.Done, "")
                        Text("Save")
                    }
                    Mode.Add -> {
                        Icon(Icons.Rounded.Add, "")
                        Text("Add")
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

class EditEmailTemplateSettingsFragmentViewModel(application: Application) :
    AndroidViewModel(application) {
    val emailTemplate: MutableLiveData<EmailTemplate<*>> = MutableLiveData()
    val showErrorIfFieldIsEmpty = mutableLiveDataOf(false)
}

private enum class Mode {
    Edit, Add
}

fun suggestEmailTemplateLabel(context: Context): String {
    val existingLabels =
        context.readPref { EmailTemplate.All.read() }.mapTo(mutableSetOf()) { it.label }
    return sequenceOf("Todo", "Work")
        .plus(generateSequence(0) { it + 1 }.map { "Todo$it" })
        .first { it !in existingLabels }
}
