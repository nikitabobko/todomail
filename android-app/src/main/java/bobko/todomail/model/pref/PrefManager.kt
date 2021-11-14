package bobko.todomail.model.pref

import bobko.todomail.util.booleanSharedPref
import bobko.todomail.util.doesSupportTiles
import bobko.todomail.util.stringSharedPref

object PrefManager {
    val prefillWithClipboardWhenStartedFromLauncher by booleanSharedPref(defaultValue = false)
    val prefillWithClipboardWhenStartedFromTile by booleanSharedPref(defaultValue = doesSupportTiles)

    val closeDialogAfterSendWhenStartedFromLauncher by booleanSharedPref(defaultValue = false)
    val closeDialogAfterSendWhenStartedFromSharesheet by booleanSharedPref(defaultValue = true)
    val closeDialogAfterSendWhenStartedFromTile by booleanSharedPref(defaultValue = true)

    val todoDraft by stringSharedPref(defaultValue = "")
}
