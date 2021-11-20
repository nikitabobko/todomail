package bobko.todomail.settings

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import bobko.todomail.R
import bobko.todomail.model.EmailTemplateRaw
import bobko.todomail.model.GoogleEmailCredential
import bobko.todomail.model.SmtpCredential
import bobko.todomail.model.UniqueEmailCredential
import bobko.todomail.settings.emailtemplate.suggestEmailTemplate
import bobko.todomail.settings.emailtemplate.suggestEmailTemplateLabel
import bobko.todomail.util.mutableLiveDataOf

class SettingsActivity : AppCompatActivity() {
    val viewModel: SettingsActivityViewModel by viewModels()
    lateinit var signInActivityForResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        signInActivityForResult = GoogleEmailCredential.registerActivityForResult(this)
    }
}

class SettingsActivityViewModel(application: Application) : AndroidViewModel(application) {
    var emailTemplateDraft = mutableLiveDataOf(suggestEmailTemplate(application))
}
