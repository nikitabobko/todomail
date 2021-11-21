package bobko.todomail.settings

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import bobko.todomail.R
import bobko.todomail.credential.suggestEmailTemplate
import bobko.todomail.model.GoogleEmailCredential
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
