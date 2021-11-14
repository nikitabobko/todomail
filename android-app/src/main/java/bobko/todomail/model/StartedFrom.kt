package bobko.todomail.model

import bobko.todomail.model.pref.PrefManager
import bobko.todomail.util.SharedPref

enum class StartedFrom(
    val text: String,
    val prefillPrefKey: SharedPref<Boolean>?,
    val closeAfterSendPrefKey: SharedPref<Boolean>
) {
    Launcher(
        "started from Launcher",
        PrefManager.prefillWithClipboardWhenStartedFromLauncher,
        PrefManager.closeDialogAfterSendWhenStartedFromLauncher
    ),
    Tile(
        "started from Tile",
        PrefManager.prefillWithClipboardWhenStartedFromTile,
        PrefManager.closeDialogAfterSendWhenStartedFromTile
    ),
    Sharesheet(
        "started from Sharesheet",
        null,
        PrefManager.closeDialogAfterSendWhenStartedFromSharesheet
    )
}
