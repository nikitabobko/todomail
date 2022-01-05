/*
 * Copyright (C) 2022 Nikita Bobko
 *
 * This file is part of Todomail.
 *
 * Todomail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Todomail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Todomail. If not, see <https://www.gnu.org/licenses/>.
 */

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
