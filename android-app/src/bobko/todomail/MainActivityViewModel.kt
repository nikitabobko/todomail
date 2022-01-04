/*
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

package bobko.todomail

import android.app.Application
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import bobko.todomail.model.StartedFrom
import bobko.todomail.model.pref.PrefManager
import bobko.todomail.util.mutableLiveDataOf
import bobko.todomail.util.readPref
import bobko.todomail.util.writePref

/**
 * Test: [bobko.todomail.MainActivityViewModelTest]
 */
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    val todoTextDraft = mutableLiveDataOf(TextFieldValue())

    init {
        application.readPref { PrefManager.todoDraft.read() }.takeIf { it.isNotBlank() }?.let {
            todoTextDraft.value = TextFieldValue(it)
        }
    }

    var todoTextDraftIsChangedAtLeastOnce = mutableLiveDataOf(false)

    private var _startedFrom: StartedFrom? = null
    var startedFrom: StartedFrom
        get() {
            return _startedFrom ?: StartedFrom.Launcher.also { _startedFrom = it }
        }
        set(value) {
            if (_startedFrom == null) {
                _startedFrom = value
            } else {
                error("The value is already initialized")
            }
        }

    private var isPrefilledWithSharedText: Boolean = false

    fun prefillSharedText(sharedText: String) {
        require(sharedText.isNotBlank())
        if (!isPrefilledWithSharedText /* Allow to prefill TextField only once */) {
            isPrefilledWithSharedText = true
            todoTextDraft.value = composeSharedText(sharedText)
        }
    }

    private fun composeSharedText(sharedText: String): TextFieldValue {
        val text = buildString {
            appendLine()
            appendLine()
            append(sharedText)
        }
        return TextFieldValue(text)
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().writePref {
            PrefManager.todoDraft.write(todoTextDraft.value.text)
        }
    }
}
