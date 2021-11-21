package bobko.todomail.model

import bobko.todomail.model.pref.PrefManager
import bobko.todomail.pref.SharedPref

enum class StartedFrom(
    val text: String,
    val prefillPrefKey: SharedPref<Boolean>?,
) {
    Launcher(
        "started from Launcher",
        PrefManager.prefillWithClipboardWhenStartedFromLauncher,
    ),
    Tile(
        "started from Tile",
        PrefManager.prefillWithClipboardWhenStartedFromTile,
    ),
    Sharesheet(
        "started from Sharesheet",
        null,
    )
}
