package bobko.todomail.model.pref

import bobko.todomail.credential.UniqueEmailCredential
import bobko.todomail.model.EmailTemplate
import bobko.todomail.pref.booleanSharedPref
import bobko.todomail.pref.stringSharedPref
import bobko.todomail.util.doesSupportTiles

object PrefManager {
    val prefillWithClipboardWhenStartedFromLauncher by booleanSharedPref(defaultValue = false)
    val prefillWithClipboardWhenStartedFromTile by booleanSharedPref(defaultValue = doesSupportTiles)

    val todoDraft by stringSharedPref(defaultValue = "")

    val all = listOf(
        prefillWithClipboardWhenStartedFromLauncher,
        prefillWithClipboardWhenStartedFromTile,
        todoDraft,
        EmailTemplate.All,
        UniqueEmailCredential.All
    )
}
