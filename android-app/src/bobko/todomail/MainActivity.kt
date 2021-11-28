package bobko.todomail

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.getSystemService
import bobko.todomail.credential.UniqueEmailCredential
import bobko.todomail.credential.sealed.EmailCredential
import bobko.todomail.credential.sealed.GoogleEmailCredential
import bobko.todomail.credential.sealed.SmtpCredential
import bobko.todomail.model.*
import bobko.todomail.settings.SettingsActivity
import bobko.todomail.theme.EmailTodoTheme
import bobko.todomail.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val viewModel by viewModels<MainActivityViewModel>()
    lateinit var signInActivityForResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        signInActivityForResult = GoogleEmailCredential.registerActivityForResult(this)

        // Some magic to show keyboard on Activity start. It
        // depends on the device whether this call is required!
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        (intent.getSerializableExtra(STARTED_FROM) as? StartedFrom)
            ?.let {
                viewModel.startedFrom = it
            }
            .orElse {
                val sharedText = intent.takeIf { it?.action == Intent.ACTION_SEND }
                    ?.getStringExtra(Intent.EXTRA_TEXT)
                if (sharedText?.isNotBlank() == true) {
                    viewModel.startedFrom = StartedFrom.Sharesheet
                    viewModel.prefillSharedText(sharedText)
                }
            }

        val templates = this.readPref {
            EmailTemplate.All.liveData
        }
        if (templates.value.count() == 0) {
            finish()
            startActivity(Intent(this, SettingsActivity::class.java)) // TODO should be deeplink?
        }
        setContent {
            MainActivityScreen(templates)
        }
        window.setGravity(Gravity.BOTTOM)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Since Android Q it's necessary for the app to have focus to be able to access clipboard.
        if (!hasFocus) {
            return
        }
        val shouldPrefillWithClipboard = when (val startedFrom = viewModel.startedFrom) {
            StartedFrom.Launcher, StartedFrom.Tile -> readPref {
                startedFrom.prefillPrefKey!!.read()
            }
            StartedFrom.Sharesheet -> false
        }
        if (!shouldPrefillWithClipboard) {
            return
        }
        val clipboardManager = getSystemService<ClipboardManager>()
        val clipboard = clipboardManager!!.primaryClip?.getItemAt(0)?.text?.toString()
        if (clipboard?.isNotBlank() != true) {
            return
        }
        viewModel.prefillSharedText(clipboard)
    }

    companion object {
        private const val STARTED_FROM = "STARTED_FROM"

        fun getIntent(context: Context, startedFrom: StartedFrom): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(STARTED_FROM, startedFrom)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainActivity.MainActivityScreen(accountsLive: InitializedLiveData<List<EmailTemplateRaw>>) {
    EmailTodoTheme {
        Column {
            // Transparent Surface for keeping space for Android context menu
            Surface(
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                    .clickable(
                        remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { finish() }
                    ),
                color = Color.Transparent
            ) {}
            Surface(
                modifier = Modifier.clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp
                    )
                )
            ) {
                TextFieldAndButtons(accountsLive)
            }
        }
    }
}

@Composable
private fun MainActivity.TextFieldAndButtons(accountsLive: InitializedLiveData<List<EmailTemplateRaw>>) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val sendInProgress = remember { mutableStateOf(false) }
        val todoTextDraft = viewModel.todoTextDraft.observeAsMutableState()
        val focusRequester = remember { FocusRequester() }
        val isError = remember { mutableStateOf(false) }

        TextField(
            value = todoTextDraft.value.copy(annotatedString = AnnotatedString.Builder(todoTextDraft.value.text)
                .apply {
                    val text = todoTextDraft.value.text
                    if (text.lineSequence().take(2).count() > 1) {
                        val style = SpanStyle(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = MaterialTheme.typography.h6.fontSize
                        )
                        addStyle(style, 0, text.lineSequence().first().length)
                    }
                }
                .toAnnotatedString()
            ),
            isError = isError.value,
            onValueChange = {
                todoTextDraft.value = it
                viewModel.todoTextDraftIsChangedAtLeastOnce.value = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            ),
            enabled = !sendInProgress.value,
            label = {
                Text(
                    when {
                        isError.value -> "Error" // TODO better message
                        sendInProgress.value -> "Sending..."
                        else -> "Your todo is..."
                    }
                )
            }
        )
        Buttons(todoTextDraft, isError, sendInProgress, accountsLive)
        DisposableEffect(sendInProgress.value, todoTextDraft.value) {
            focusRequester.requestFocus()
            onDispose { }
        }
    }
}

@Composable
private fun MainActivity.Buttons(
    todoTextDraft: MutableState<TextFieldValue>,
    isError: MutableState<Boolean>,
    sendInProgress: MutableState<Boolean>,
    accountsLive: InitializedLiveData<List<EmailTemplateRaw>>
) {
    val canStartSending = !sendInProgress.value && todoTextDraft.value.text.isNotBlank()
    val unspecifiedOrErrorColor =
        if (isError.value && canStartSending) MaterialTheme.colors.error else Color.Unspecified
    val greenOrErrorColor =
        if (isError.value) MaterialTheme.colors.error else MaterialTheme.colors.primary
    val scope = rememberCoroutineScope()
    CenteredRow {
        TextButton(
            onClick = {
                todoTextDraft.value = TextFieldValue()
                isError.value = false
            },
            enabled = canStartSending
        ) { Text(text = "Clear", color = unspecifiedOrErrorColor) }
        IconButton(onClick = {
            startActivity(Intent(this@Buttons, SettingsActivity::class.java))
        }) { Icon(Icons.Rounded.Settings, "", tint = greenOrErrorColor) }

        Spacer(modifier = Modifier.weight(1f))

        val onClick = { emailTemplate: EmailTemplateRaw ->
            sendButtonClicked(this@Buttons, scope, todoTextDraft, sendInProgress, emailTemplate, isError)
        }
        val minButtonsToStartFolding = 4
        val numOfButtonsToFoldDownTo = 2
        val accounts by accountsLive.observeAsState()
        val doFold = accounts.count() >= minButtonsToStartFolding
        if (doFold) {
            Box {
                var expanded by remember { mutableStateOf(false) }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = false) // Don't hide keyboard
                ) {
                    accounts.drop(numOfButtonsToFoldDownTo)
                        .forEach {
                            DropdownMenuItem(onClick = { onClick(it) }) {
                                Text(it.label, color = greenOrErrorColor)
                            }
                        }
                }
                IconButton(onClick = { expanded = true }, enabled = canStartSending) {
                    if (canStartSending) {
                        Icon(Icons.Rounded.MoreVert, "", tint = greenOrErrorColor)
                    } else {
                        Icon(Icons.Rounded.MoreVert, "")
                    }
                }
            }
        }

        accounts.let { if (doFold) it.take(numOfButtonsToFoldDownTo) else it }
            .reversed()
            .forEach {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { onClick(it) }, enabled = canStartSending) {
                    Text(it.label, color = unspecifiedOrErrorColor)
                }
            }
    }
}

private fun sendButtonClicked(
    activity: MainActivity,
    scope: CoroutineScope,
    todoTextDraft: MutableState<TextFieldValue>,
    sendInProgress: MutableState<Boolean>,
    emailTemplate: EmailTemplateRaw,
    isError: MutableState<Boolean>
) {
    scope.launch {
        val prevText = todoTextDraft.value.text
        sendInProgress.value = true
        todoTextDraft.value = TextFieldValue()
        try {
            sendEmailWithTokenRefreshAttempt(prevText, emailTemplate, activity)
            isError.value = false
            activity.showToast("Successful!")
            when (activity.viewModel.startedFrom) {
                StartedFrom.Launcher -> Unit
                StartedFrom.Tile -> activity.finish()
                StartedFrom.Sharesheet -> activity.finish()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace() // TODO
            todoTextDraft.value = TextFieldValue(prevText)
            isError.value = true
        } finally {
            sendInProgress.value = false
        }
    }
}

private suspend fun sendEmailWithTokenRefreshAttempt(
    prevText: String,
    emailTemplate: EmailTemplateRaw,
    activity: MainActivity
) {
    val subject = prevText.lineSequence().first()
    val body = prevText.lineSequence().drop(1).joinToString("\n").trim()
    try {
        emailTemplate.sendEmail(activity, subject, body)
    } catch (ex: Throwable) {
        val invalidUniqCred = emailTemplate.uniqueCredential
        when (val invalidCred = invalidUniqCred.credential) {
            is GoogleEmailCredential -> {
                val newCred = invalidCred.tryRefreshOauthToken()
                    ?: GoogleEmailCredential.signIn(activity, activity.signInActivityForResult)
                    ?: errorException(ex)
                replaceCredential(emailTemplate, newCred, activity).sendEmail(activity, subject, body)
            }
            is SmtpCredential -> {
                errorException(ex)
            }
        }
    }
}

private fun replaceCredential(
    invalidTemplate: EmailTemplateRaw,
    newCred: EmailCredential,
    context: Context
): EmailTemplate<EmailCredential> {
    val invalidUniqCred = invalidTemplate.uniqueCredential
    val newTemplate = invalidTemplate.switchCredential(
        UniqueEmailCredential.new(newCred, context, forceNew = true),
        context
    )
    context.writePref {
        val newTemplates = EmailTemplate.All.read().map { template ->
            when (template.id) {
                invalidTemplate.id -> newTemplate
                else -> when (template.uniqueCredential.id) {
                    invalidUniqCred.id -> template.switchCredential(
                        newTemplate.uniqueCredential,
                        context
                    )
                    else -> template
                }
            }
        }
        EmailTemplate.All.write(newTemplates)
    }
    return newTemplate
}
