package bobko.todomail.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import bobko.todomail.model.*
import bobko.todomail.settings.emailtemplate.suggestEmailTemplate
import bobko.todomail.settings.emailtemplate.suggestEmailTemplateLabel
import bobko.todomail.theme.EmailTodoTheme
import bobko.todomail.util.CenteredRow
import bobko.todomail.util.composeView
import bobko.todomail.util.observeAsState
import bobko.todomail.util.readPref
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ChooseEmailCredentialTypeDialogFragment : BottomSheetDialogFragment() {
    fun parentActivity() = requireActivity() as SettingsActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = requireContext().composeView {
        AddAccountSettingsWizardFragmentScreen()
    }
}

@Composable
fun ChooseEmailCredentialTypeDialogFragment.AddAccountSettingsWizardFragmentScreen() {
    EmailTodoTheme {
        Surface {
            Column {
                Items()
                CenteredRow(modifier = Modifier.padding(8.dp)) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { findNavController().navigateUp() }) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

private class CredentialTypeComparator : Comparator<EmailCredential> {
    override fun compare(x: EmailCredential, y: EmailCredential): Int {
        fun EmailCredential.index() = when (this) {
            GoogleEmailCredential -> 0
            is SmtpCredential -> 1
        }
        return x.index().compareTo(y.index())
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ChooseEmailCredentialTypeDialogFragment.Items() {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        val existingCredentials = requireContext().readPref { UniqueEmailCredential.All.liveData }
            .observeAsState()
            .value
            .sortedWith(
                compareBy<UniqueEmailCredential<*>, EmailCredential>(CredentialTypeComparator()) { it.credential }
                    .thenBy { it.credential.getLabel(requireContext()) }
            )

        existingCredentials.forEach {
            MailItem(
                text = it.credential.getLabel(requireContext()),
                uniqueEmailCredential = it
            )
        }

        if (existingCredentials.isNotEmpty()) {
            Divider()
        }

        AnimatedVisibility(visible = !GoogleEmailCredential.isSigned(requireContext()).observeAsState().value) {
            MailItem(
                text = GoogleEmailCredential.getLabel(requireContext()),
                uniqueEmailCredential = UniqueEmailCredential.new(GoogleEmailCredential, requireContext())
            )
        }

        KnownSmtpCredential.values().forEach {
            MailItem(
                text = it.label,
                uniqueEmailCredential = UniqueEmailCredential.new(it.smtpCredential, requireContext())
            )
        }
        MailItem(
            "Email (SMTP)",
            uniqueEmailCredential = UniqueEmailCredential.new(SmtpCredential.default, requireContext())
        )
    }
}

@Composable
fun DefaultEmailIcon() {
    Icon(
        Icons.Rounded.Email,
        "Email (SMTP)",
        modifier = Modifier.size(emailIconSize)
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ChooseEmailCredentialTypeDialogFragment.MailItem(
    text: String,
    uniqueEmailCredential: UniqueEmailCredential<*>
) {
    ListItem(
        modifier = Modifier.clickable {
            lifecycle.coroutineScope.launch {
                val success = if (uniqueEmailCredential.credential == GoogleEmailCredential &&
                    !GoogleEmailCredential.isSigned(requireContext()).value
                ) {
                    GoogleEmailCredential.signIn(requireContext(), parentActivity().signInActivityForResult)
                        ?.let { true }
                        ?: false
                } else {
                    true
                }
                if (success) {
                    parentActivity().viewModel.emailTemplateDraft.value = EmailTemplate.new(
                        suggestEmailTemplateLabel(requireContext()),
                        "",
                        uniqueEmailCredential,
                        requireContext()
                    )
                    findNavController().navigateUp()
                }
            }
        },
        trailing = {
            if (uniqueEmailCredential.credential == GoogleEmailCredential && GoogleEmailCredential.isSigned(requireContext()).value) {
                TextButton(
                    onClick = {
                        lifecycle.coroutineScope.launch {
                            GoogleEmailCredential.signOut(requireContext())
                            parentActivity().viewModel.emailTemplateDraft.value = suggestEmailTemplate(requireContext())
                        }
                    }
                ) {
                    Text("Sign out")
                }
            }
        },
        icon = {
            uniqueEmailCredential.credential.Icon()
        }
    ) {
        Text(text)
    }
}
