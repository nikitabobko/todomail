package bobko.todomail.settings.emailtemplate

import android.app.Application
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
import bobko.todomail.credential.suggestUniqueCredential
import bobko.todomail.model.*
import bobko.todomail.settings.SettingsActivity
import bobko.todomail.settings.SettingsScreen
import bobko.todomail.util.*

class EditEmailTemplateSettingsFragment : Fragment() {
    val viewModel by viewModels<EditEmailTemplateSettingsFragmentViewModel>()
    fun parentActivity() = requireActivity() as SettingsActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = requireContext().composeView {
        val existingTemplates = requireContext().readPref { EmailTemplate.All.read() }
        val emailTemplateDraftLive = parentActivity().viewModel.emailTemplateDraft
        val mode =
            if (emailTemplateDraftLive.value.id in existingTemplates.mapTo(mutableSetOf()) { it.id }) Mode.Edit
            else Mode.Add
        // TODO add screen rotation test
        val emailTemplate = emailTemplateDraftLive.observeAsMutableState()

        // Whenever the activity is called on disposed credential (for example it may become disposed when user logs out),
        // we replace disposed credential with suggested valid credential
        if (emailTemplate.value.uniqueCredential.isDisposed(requireContext())) {
            emailTemplate.value = emailTemplate.value.switchCredential(
                suggestUniqueCredential(requireContext()),
                requireContext()
            )
        }

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
    SettingsScreen("${mode.string} Email Template") {
        val label = remember { LabelTextFieldItem(emailTemplate.value, requireContext()) }
        val sendTo = remember { SendToTextFieldItem<TEmailCredential>() }
        val smtpFields = remember { getSmtpFields() }
        val allFields = when (emailTemplate.value.uniqueCredential.credential as EmailCredential) {
            is GoogleEmailCredential -> listOf(label, sendTo)
            is SmtpCredential -> {
                (listOf(label) + smtpFields + sendTo) as List<TextFieldItem<*, TEmailCredential>>
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        label.Content(emailTemplate, allFields, viewModel)
        MyTextDivider("Credentials settings")
        CredentialsTypeSpinner(emailTemplate)

        AnimatedContent(
            targetState = TargetWithContext(
                emailTemplate.value.uniqueCredential.credential::class,
                emailTemplate.value to allFields
            )
        ) { (targetState, context) ->
            if (targetState == SmtpCredential::class) {
                val fakeTemplateForAnimation = context.first
                val fakeFieldsForAnimation = context.second as List<TextFieldItem<*, SmtpCredential>>
                Column {
                    smtpFields.forEach { field ->
                        field.Content(
                            emailTemplate.takeIf { it.value.uniqueCredential.credential is SmtpCredential }
                                .orElse { mutableStateOf(fakeTemplateForAnimation) }
                                .cast<MutableState<EmailTemplate<SmtpCredential>>>(),
                            fakeFieldsForAnimation,
                            viewModel
                        )
                    }
                }
            }
        }

        MyTextDivider("Destination address settings")
        sendTo.Content(emailTemplate, allFields, viewModel)
        Buttons(emailTemplate, allFields, mode)
    }
}

@Composable
private fun <TEmailCredential : EmailCredential> EditEmailTemplateSettingsFragment.CredentialsTypeSpinner(
    emailTemplate: MutableState<EmailTemplate<TEmailCredential>>
) {
    val credential = emailTemplate.value.uniqueCredential.credential
    Spinner(
        label = credential.label,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp),
        onClick = {
            findNavController().navigate(
                R.id.action_editEmailTemplateSettingsFragment_to_chooseEmailCredentialTypeDialogFragment
            )
        },
        leadingIcon = {
            Crossfade(targetState = credential.type) { it.Icon() }
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
    emailTemplate: MutableState<EmailTemplate<TEmailCredential>>,
    textFields: List<TextFieldItem<*, TEmailCredential>>,
    mode: Mode,
) {
    Spacer(modifier = Modifier.height(16.dp))
    CenteredRow(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
        if (mode == Mode.Edit) {
            OutlinedButton(
                onClick = {
                    requireContext().writePref {
                        EmailTemplate.All.write(EmailTemplate.All.read().filter { it.id != emailTemplate.value.id })
                    }
                    findNavController().navigateUp()
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
                        Text(mode.string)
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

private enum class Mode(val string: String) {
    Edit("Edit"), Add("Add")
}
