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

    val todoDraft by stringSharedPref(defaultValue = "")

    val all = listOf(
        prefillWithClipboardWhenStartedFromLauncher,
        prefillWithClipboardWhenStartedFromTile,
        todoDraft,
        UniqueEmailCredential.uniqueCredentialId,
        EmailTemplate.uniqueEmailTemplateId,
        LastUsedAppFeatureManager.appendAppNameThatSharedTheText,
        LastUsedAppFeatureManager.isUsageAccessPromptShowedAtLeastOnce,
        EmailTemplate.All,
        UniqueEmailCredential.All
    )
}
