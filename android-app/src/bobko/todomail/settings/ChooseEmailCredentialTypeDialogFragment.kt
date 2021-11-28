package bobko.todomail.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import bobko.todomail.credential.*
import bobko.todomail.credential.sealed.EmailCredential
import bobko.todomail.credential.sealed.GoogleEmailCredential
import bobko.todomail.credential.sealed.type
import bobko.todomail.model.EmailTemplate
import bobko.todomail.theme.EmailTodoTheme
import bobko.todomail.util.*
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

    fun navigateToEditTemplatePage(uniqueCredential: UniqueEmailCredential<*>) {
        parentActivity().viewModel.emailTemplateDraft.let {
            it.value = it.value.switchCredential(uniqueCredential, requireContext())
        }
        findNavController().navigateUp()
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ChooseEmailCredentialTypeDialogFragment.Items() {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        val existingCredentials = requireContext().readPref { UniqueEmailCredential.All.liveData }
            .observeAsState()
            .value
            .sortedWith(compareBy(PreferredEmailCredentialComparator(requireContext())) { it.credential })

        existingCredentials
            .filter { !it.credential.isEmpty }
            .forEach {
                MailItem(it.credential.label, it)
            }

        if (existingCredentials.isNotEmpty()) {
            Divider()
        }

        SignInMailItem(
            text = "Sign in with Google",
            credentialType = GoogleCredentialType,
            createCredential = {
                GoogleEmailCredential.signIn(requireContext(), parentActivity().signInActivityForResult)
                    ?.let { UniqueEmailCredential.new(it, requireContext()) }
            }
        )
        SmtpCredentialType.values().forEach {
            SignInMailItem(
                text = it.label,
                credentialType = it.smtpCredential.type,
                createCredential = { UniqueEmailCredential.new(it.smtpCredential, requireContext()) }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun <T : EmailCredential> ChooseEmailCredentialTypeDialogFragment.SignInMailItem(
    text: String,
    credentialType: EmailCredentialType<T>,
    createCredential: suspend () -> UniqueEmailCredential<T>?
) {
    ListItem(
        modifier = Modifier.clickable {
            lifecycle.coroutineScope.launch {
                createCredential()?.let {
                    requireContext().writePref {
                        UniqueEmailCredential.All.write(UniqueEmailCredential.All.read() + it)
                    }
                    navigateToEditTemplatePage(it)
                }
            }
        },
        icon = { credentialType.Icon() }
    ) {
        Text(text)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ChooseEmailCredentialTypeDialogFragment.MailItem(
    text: String,
    uniqueCredential: UniqueEmailCredential<*>
) {
    var alertMessage by remember { mutableStateOf("") }
    val signOut = {
        requireContext().writePref {
            EmailTemplate.All.write(EmailTemplate.All.read()
                .filter { it.uniqueCredential.id != uniqueCredential.id })
        }
    }
    if (alertMessage.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { alertMessage = "" },
            buttons = {
                CenteredRow(modifier = Modifier.padding(8.dp)) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { alertMessage = "" }) {
                        Text("Cancel")
                    }
                    TextButton(onClick = { signOut() }) {
                        Text("Ok")
                    }
                }
            },
            text = {
                Text(alertMessage)
            }
        )
    }
    ListItem(
        modifier = Modifier.clickable { navigateToEditTemplatePage(uniqueCredential) },
        trailing = {
            TextButton(
                onClick = {
                    val templatesToDelete = requireContext().readPref { EmailTemplate.All.read() }
                        .filter { it.uniqueCredential.id == uniqueCredential.id }
                    if (templatesToDelete.isNotEmpty()) {
                        alertMessage = "Signing out of '${uniqueCredential.credential.label}' leads to " +
                                "${templatesToDelete.size} template${if (templatesToDelete.size > 1) "s" else ""} removal: " +
                                templatesToDelete.joinToString { "'${it.label}'" } + "\nContinue?"
                    } else {
                        signOut()
                    }
                }
            ) {
                Text("Sign out")
            }
        },
        icon = { uniqueCredential.credential.type.Icon() }
    ) {
        Text(text)
    }
}
