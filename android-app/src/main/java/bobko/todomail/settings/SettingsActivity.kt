package bobko.todomail.settings

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import bobko.todomail.R
import bobko.todomail.credential.sealed.GoogleEmailCredential
import bobko.todomail.credential.suggestEmailTemplate
import bobko.todomail.model.EmailTemplate
import bobko.todomail.util.PrefWriterDslReceiver
import bobko.todomail.util.mutableLiveDataOf
import bobko.todomail.util.writePref

class SettingsActivity : AppCompatActivity() {
    val viewModel: SettingsActivityViewModel by viewModels()
    lateinit var signInActivityForResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        signInActivityForResult = GoogleEmailCredential.registerActivityForResult(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        writePref { garbageCollectUnreachableCredentials() }
    }

    private fun PrefWriterDslReceiver.garbageCollectUnreachableCredentials() {
        EmailTemplate.All.write(EmailTemplate.All.read())
    }
}

class SettingsActivityViewModel(application: Application) : AndroidViewModel(application) {
    var emailTemplateDraft = mutableLiveDataOf(suggestEmailTemplate(application))
}
