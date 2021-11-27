package bobko.todomail.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import bobko.todomail.R
import bobko.todomail.credential.SmtpCredentialType
import bobko.todomail.credential.emailIconSize
import bobko.todomail.credential.suggestEmailTemplate
import bobko.todomail.model.*
import bobko.todomail.model.pref.LastUsedAppFeatureManager
import bobko.todomail.util.*

class MainSettingsFragment : Fragment() {
    fun parentActivity() = requireActivity() as SettingsActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireContext().readPref { EmailTemplate.All.read() }.isEmpty()) {
            navigateToEditScreen()
        }
    }

    fun navigateToEditScreen(emailTemplateToEdit: EmailTemplateRaw? = null) {
        parentActivity().viewModel.emailTemplateDraft.value = emailTemplateToEdit ?: suggestEmailTemplate(requireContext())
        findNavController().navigate(
            R.id.action_mainSettingsFragment_to_editEmailTemplateSettingsFragment
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = requireContext().composeView {
        MainSettingsActivityScreen(requireContext().readPref { EmailTemplate.All.liveData })
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainSettingsFragment.MainSettingsActivityScreen(accounts: InitializedLiveData<List<EmailTemplateRaw>>) {
    SettingsScreen("Todomail Settings", rootSettingsScreen = true) {
        TextDivider("Templates")
        TemplatesSection(accounts)

        Divider()
        TextDivider("Prefill with clipboard when the app is")
        WhenTheAppIsStartedFromSection(listOf(
            StartedFrom.Launcher.let { it to it.prefillPrefKey!! },
            StartedFrom.Tile.let { it to it.prefillPrefKey!! }
        ))

        Divider()
        OtherSettingsSection()

        OutlinedButton(
            onClick = { /*TODO*/ }, modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Reset settings to default")
        }
    }
}

@Composable
private fun MainSettingsFragment.OtherSettingsSection() {
    val append by LastUsedAppFeatureManager.isFeatureEnabled(requireContext())
        .observeAsState()
    SwitchOrCheckBoxItem(
        "Append app name that shared the text or clipboard",
        checked = append,
        onChecked = {
            this.requireContext().writePref {
//                PrefManager.appendAppNameThatSharedTheText.value = !append TODO
            }
        }
    )
}

/**
 * Test - [bobko.todomail.settings.CalculateIndexOffsetTest]
 */
fun calculateIndexOffset(pixelOffset: Int, itemHeight: Int) =
    (pixelOffset / (itemHeight / 2)).let { it / 2 + it % 2 }

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MainSettingsFragment.TemplatesSection(
    emailTemplatesLive: InitializedLiveData<List<EmailTemplateRaw>>
) {
    val templates by emailTemplatesLive.observeAsState()
    var offsets by remember(templates.size) { mutableStateOf(List(templates.size) { 0 }) }
    val itemHeightDp = 80.dp // TODO rewrite!
    val itemHeight = with(LocalDensity.current) { itemHeightDp.toPx().toInt() }
    templates.forEachIndexed { currentIdx, emailTemplate ->
        val offsetLowerBound = -currentIdx * itemHeight
        val offsetUpperBound = (templates.lastIndex - currentIdx) * itemHeight
        ListItem(
            icon = {
                SmtpCredentialType.values() // TODO icon by domain, hmm. Is it okay logic?
                    .singleOrNull { emailTemplate.sendTo.endsWith(it.domain ?: return@singleOrNull false) }
                    .orElse { SmtpCredentialType.Generic }
                    .Icon()
            },
            modifier = Modifier
                .offset(y = with(LocalDensity.current) { offsets[currentIdx].toDp() })
                .clickable { navigateToEditScreen(emailTemplate) }
                .height(itemHeightDp),
            trailing = trailing@{
                if (templates.size <= 1) {
                    return@trailing
                }
                Icon(
                    painterResource(R.drawable.drag_handle_24),
                    "",
                    // TODO Change to swipeable? https://developer.android.com/jetpack/compose/gestures#swiping
                    modifier = Modifier.draggable(
                        rememberDraggableState(onDelta = { delta ->
                            val newOffset = (offsets[currentIdx] + delta.toInt())
                                .coerceIn(offsetLowerBound..offsetUpperBound)
                            val newIdx = currentIdx + calculateIndexOffset(newOffset, itemHeight)
                            offsets = List(offsets.size) {
                                when (it) {
                                    currentIdx -> newOffset

                                    in minOf(newIdx, currentIdx)..maxOf(newIdx, currentIdx) -> {
                                        sign(currentIdx - it) * itemHeight
                                    }

                                    else -> 0
                                }
                            }
                        }),
                        orientation = Orientation.Vertical,
                        onDragStopped = {
                            val newIdx = currentIdx + calculateIndexOffset(
                                offsets[currentIdx],
                                itemHeight
                            )

                            val newAccounts = templates.toMutableList().apply {
                                removeAt(currentIdx)
                                add(newIdx, emailTemplate)
                            }

                            offsets = List(templates.size) { 0 }
                            requireContext().writePref { EmailTemplate.All.write(newAccounts) }
                        }
                    )
                )
            },
            text = {
                Text(text = "[${emailTemplate.label}] TO: ${emailTemplate.sendTo}")
            },
            secondaryText = {
                Text("FROM: ${emailTemplate.uniqueCredential.credential.label}")
            }
        )
    }
    ListItem(
        icon = {
            Icon(
                Icons.Rounded.Add,
                "",
                modifier = Modifier.size(emailIconSize)
            )
        },
        modifier = Modifier.clickable {
            navigateToEditScreen()
        },
        text = { Text(text = "Add template") }
    )
}
