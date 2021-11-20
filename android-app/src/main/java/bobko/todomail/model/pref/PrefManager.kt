package bobko.todomail.model.pref

import bobko.todomail.model.EmailTemplate
import bobko.todomail.model.EmailTemplateRaw
import bobko.todomail.model.UniqueEmailCredential
import bobko.todomail.pref.booleanSharedPref
import bobko.todomail.util.doesSupportTiles
import bobko.todomail.pref.stringSharedPref

object PrefManager {
    val prefillWithClipboardWhenStartedFromLauncher by booleanSharedPref(defaultValue = false)
    val prefillWithClipboardWhenStartedFromTile by booleanSharedPref(defaultValue = doesSupportTiles)

    val closeDialogAfterSendWhenStartedFromLauncher by booleanSharedPref(defaultValue = false)
    val closeDialogAfterSendWhenStartedFromSharesheet by booleanSharedPref(defaultValue = true)
    val closeDialogAfterSendWhenStartedFromTile by booleanSharedPref(defaultValue = true)

    val todoDraft by stringSharedPref(defaultValue = "")

    val all = listOf(
        prefillWithClipboardWhenStartedFromLauncher,
        prefillWithClipboardWhenStartedFromTile,
        closeDialogAfterSendWhenStartedFromLauncher,
        closeDialogAfterSendWhenStartedFromSharesheet,
        closeDialogAfterSendWhenStartedFromTile,
        todoDraft,
        UniqueEmailCredential.uniqueCredentialId,
        LastUsedAppFeatureManager.appendAppNameThatSharedTheText,
        LastUsedAppFeatureManager.isUsageAccessPromptShowedAtLeastOnce,
        EmailTemplate.All,
        UniqueEmailCredential.All
    )
}
