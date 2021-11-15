package bobko.todomail.settings.emailtemplate

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import bobko.todomail.model.EmailTemplate
import bobko.todomail.model.SmtpCredential
import bobko.todomail.model.UniqueEmailCredential
import bobko.todomail.settings.SettingsActivity
import bobko.todomail.settings.SettingsScreen
import bobko.todomail.util.*

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
        EditEmailTemplateSettingsFragmentScreen(mode)
    }
}

@Composable
private fun EditEmailTemplateSettingsFragment.EditEmailTemplateSettingsFragmentScreen(mode: Mode) {
    SettingsScreen("Edit Email Template Settings") {
        val emailTemplate =
            viewModel.emailTemplate.observeAsMutableState { // TODO add screen rotation test
                parentActivity().viewModel.emailTemplateToEdit ?: EmailTemplate(
                    suggestEmailTemplateLabel(requireContext()), "",
                    UniqueEmailCredential.new(
                        SmtpCredential("", DEFAULT_SMTP_PORT, "", ""),
                        requireContext()
                    )
                )
            }

        Spacer(modifier = Modifier.height(16.dp))
        viewModel.schema.forEach {
            it.Composable(emailTemplate, viewModel)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Buttons(viewModel.schema, emailTemplate, mode)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun EditEmailTemplateSettingsFragment.Buttons(
    schema: List<Item>,
    emailTemplate: MutableState<EmailTemplate>,
    mode: Mode,
) {
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
                if (schema.filterIsInstance<TextFieldItem<*>>().any { item ->
                        item.getCurrentText(emailTemplate.value)
                            .let { it.isBlank() || item.errorProvider(it) != null }
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
}

class EditEmailTemplateSettingsFragmentViewModel(application: Application) :
    AndroidViewModel(application) {
    val emailTemplate: MutableLiveData<EmailTemplate> = MutableLiveData()
    val showErrorIfFieldIsEmpty = mutableLiveDataOf(false)
    val schema: List<Item> = getSchema(
        existingLabels = application.readPref { EmailTemplate.All.read() }
            .mapTo(mutableSetOf()) { it.label }
    )
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
