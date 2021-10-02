package bobko.email.todo

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.core.content.getSystemService
import bobko.email.todo.util.getAppLabelByPackageName
import bobko.email.todo.util.orElse
import bobko.email.todo.util.readPref

object LastUsedAppProvider {
    fun getLastUsedAppLabel(context: Context): String? {
        val time = System.currentTimeMillis()
        val stats = context.getSystemService<UsageStatsManager>()!!
            .queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 10, // We get usage stats for the last 10 seconds
                time
            )
        val packageName = stats.asSequence()
            .filter { it.packageName != context.packageName && it.packageName != "android" }
            .maxByOrNull { it.lastTimeUsed }
            .orElse { return null }
            .packageName
        return context.getAppLabelByPackageName(packageName)
    }

    fun isFeatureEnabled(context: Context) = isUsageAccessGranted(context) &&
            context.readPref { PrefManager.appendAppNameThatSharedTheText.value }

    private fun isUsageAccessGranted(context: Context): Boolean {
        val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
        val appOpsManager = context.getSystemService<AppOpsManager>()!!
        return AppOpsManager.MODE_ALLOWED == appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            applicationInfo.uid, applicationInfo.packageName
        )
    }
}
