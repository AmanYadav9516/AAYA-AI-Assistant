package com.aaya.assistant.engine.lifestyle

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.R
import com.aaya.assistant.data.local.AayaDatabase
import com.aaya.assistant.data.model.RoutineModel
import com.aaya.assistant.engine.router.DeviceController
import java.util.Calendar

class SleepIntelligenceEngine(private val context: Context) {

    private val deviceController = DeviceController(context)
    private val db = AayaDatabase.getInstance(context)

    suspend fun checkAndApplyRoutines() {
        val activeRoutines = db.aayaDao().getActiveRoutines()
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeMinutes = currentHour * 60 + currentMinute

        for (routine in activeRoutines) {
            val startMinutes = parseMinutes(routine.startTime)
            val endMinutes = parseMinutes(routine.endTime)

            val isInWindow = if (startMinutes <= endMinutes) {
                currentTimeMinutes in startMinutes..endMinutes
            } else {
                // Crosses midnight (e.g., 23:00 to 07:00)
                currentTimeMinutes >= startMinutes || currentTimeMinutes <= endMinutes
            }

            if (isInWindow && routine.autoSilenceNotifications) {
                deviceController.setDoNotDisturb(true)
            }
        }
    }

    fun promptSleepModeSuggestion(predictedBedtime: String = "11:20 PM") {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, AayaApplication.CHANNEL_LIFESTYLE_ALERTS)
            .setContentTitle("🌙 Bedtime Approaching")
            .setContentText("Your usual sleep time ($predictedBedtime) is approaching. Activate Sleep Mode?")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_SLEEP, notification)
    }

    private fun parseMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    companion object {
        private const val NOTIFICATION_ID_SLEEP = 2001
    }
}
