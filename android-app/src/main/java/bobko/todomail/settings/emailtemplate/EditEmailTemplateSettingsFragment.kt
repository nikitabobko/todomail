package bobko.todomail.settings.emailtemplate

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.fragment.findNavController
import bobko.todomail.R
import bobko.todomail.model.*
import bobko.todomail.settings.DefaultEmailIcon
import bobko.todomail.settings.SettingsActivity
import bobko.todomail.settings.SettingsScreen
import bobko.todomail.util.*

class EditEmailTemplateSettingsFragment : Fragment() {
    val viewModel by viewModels<EditEmailTemplateSettingsFragmentViewModel>()
    private fun parentActivity() = requireActivity() as SettingsActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = requireContext().composeView {
        val existingTemplates = requireContext().readPref { EmailTemplate.All.read() }
        val mode =
            if (parentActivity().viewModel.emailTemplateDraft.value in existingTemplates) Mode.Edit
            else Mode.Add
        // TODO add screen rotation test
        val emailTemplate = parentActivity().viewModel.emailTemplateDraft.observeAsMutableState()

        EditEmailTemplateSettingsFragmentScreen(emailTemplate, mode)
    }
}

private fun getSmtpFields(): List<TextFieldItem<*, SmtpCredential>> = listOf(
    SmtpServerTextField(),
    SmtpServerPortTextFieldItem(),
    UsernameTextFieldItem(),
    PasswordTextFieldItem(),
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun <TEmailCredential : EmailCredential> EditEmailTemplateSettingsFragment.EditEmailTemplateSettingsFragmentScreen(
    emailTemplate: MutableState<EmailTemplate<TEmailCredential>>,
    mode: Mode
) {
    SettingsScreen("Edit Email Template Settings") {
        val label = remember { LabelTextFieldItem<TEmailCredential>(requireContext()) }
        val smtpFields = remember { getSmtpFields() }
        val fields: List<TextFieldItem<*, TEmailCredential>> = when (emailTemplate.value.uniqueCredential.credential) {
            GoogleEmailCredential -> listOf()
            is SmtpCredential -> smtpFields as List<TextFieldItem<*, TEmailCredential>>
            else -> error("Unknown credential type")
        }
        val destinationAddress = remember { SendToTextFieldItem<TEmailCredential>() }
        val schema = listOf(label) + fields + destinationAddress

        Spacer(modifier = Modifier.height(16.dp))
        label.Content(emailTemplate, viewModel)
        MyTextDivider("Credentials settings")
        CredentialsTypeSpinner(emailTemplate)

        var lastUsedSmtpForAnimation by remember {
            mutableStateOf(
                EmailTemplate(
                    emailTemplate.value.label,
                    emailTemplate.value.sendTo,
                    UniqueEmailCredential.new(SmtpCredential.default, requireContext())
                )
            )
        }

        AnimatedVisibility(
            visible = fields.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                when (emailTemplate.value.uniqueCredential.credential) {
                    GoogleEmailCredential -> {
                        smtpFields.forEach { it.Content(mutableStateOf(lastUsedSmtpForAnimation), viewModel) }
                    }
                    is SmtpCredential -> {
                        emailTemplate as MutableState<EmailTemplate<SmtpCredential>>
                        lastUsedSmtpForAnimation = emailTemplate.value
                        smtpFields.forEach { it.Content(emailTemplate, viewModel) }
                    }
                }
            }
        }
        MyTextDivider("Destination address settings")
        destinationAddress.Content(emailTemplate, viewModel)
        Buttons(schema, emailTemplate, mode)
    }
}

@Composable
private fun <TEmailCredential : EmailCredential> EditEmailTemplateSettingsFragment.CredentialsTypeSpinner(
    emailTemplate: MutableState<EmailTemplate<TEmailCredential>>
) {
    val credential = emailTemplate.value.uniqueCredential.credential
    Spinner(
        label = credential.getLabel(requireContext()),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp),
        onClick = {
            findNavController().navigate(
                R.id.action_editEmailTemplateSettingsFragment_to_chooseEmailCredentialTypeDialogFragment
            )
        },
        leadingIcon = {
            val targetState = when (credential) {
                GoogleEmailCredential -> credential
                is SmtpCredential -> KnownSmtpCredential.findBySmtpServer(credential)
                else -> error("Unknown credential type")
            }
            Crossfade(targetState = targetState) {
                it?.Icon() ?: DefaultEmailIcon()
            }
        },
        trailingIcon = {
            Icon(Icons.Filled.ArrowDropDown, "", modifier = Modifier.size(emailIconSize))
        }
    )
}

@Composable
private fun MyTextDivider(text: String) {
    Spacer(modifier = Modifier.height(8.dp))
    TextDivider(text)
}

@Composable
private fun <TEmailCredential : EmailCredential> EditEmailTemplateSettingsFragment.Buttons(
    textFields: List<TextFieldItem<*, TEmailCredential>>,
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
                            .let { it.isBlank() || field.getErrorIfAny(it) != null }
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

fun suggestEmailTemplate(context: Context): EmailTemplateRaw {
    return EmailTemplate(
        suggestEmailTemplateLabel(context),
        "",
        UniqueEmailCredential.new(
            if (GoogleEmailCredential.isSigned(context).value) GoogleEmailCredential else SmtpCredential.default,
            context
        )
    )
}
