package com.aaya.assistant.engine.wellness

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import com.aaya.assistant.AayaApplication
import java.util.Calendar

class UsagePoliceManager(private val context: Context) {

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun checkAndEnforceAppLimits() {
        if (!hasUsagePermission()) return

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return

        val app = context.applicationContext as? AayaApplication
        val limitMinutes = app?.preferenceManager?.instagramDailyLimitMinutes ?: 60
        val limitMs = limitMinutes * 60 * 1000L

        for (usage in stats) {
            if (usage.packageName == "com.instagram.android") {
                val totalTimeForeground = usage.totalTimeInForeground
                if (totalTimeForeground > limitMs) {
                    enforceAppLimit(usage.packageName, "Instagram", totalTimeForeground / 60000)
                    break
                }
            }
        }
    }

    private fun enforceAppLimit(packageName: String, appName: String, minutesUsed: Long) {
        val app = context.applicationContext as? AayaApplication
        val userName = app?.preferenceManager?.userName ?: "there"

        // Speak warning
        val msg = "$userName, $appName ka daily time ($minutesUsed minutes) pura ho gaya hai! Padhaai aur health par dhyan do."
        app?.ttsManager?.speak(msg)
        app?.showSystemNotification("App Usage Police", msg)

        // Close app by redirecting to Home Screen
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)
    }
}
