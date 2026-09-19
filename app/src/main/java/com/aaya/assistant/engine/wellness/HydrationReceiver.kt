package com.aaya.assistant.engine.wellness

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aaya.assistant.AayaApplication
import java.util.Calendar

class HydrationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = AayaApplication.instance.preferenceManager
        if (!prefs.isWaterReminderEnabled) return

        val userName = prefs.userName.ifBlank { "there" }
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Only remind during active waking hours (8 AM to 10 PM)
        if (hour in 8..22) {
            val isRestTime = hour % 4 == 0
            if (isRestTime) {
                AayaApplication.instance.showSystemNotification(
                    title = "🧘 Time for a Rest, $userName",
                    message = "Hey $userName, you've been working hard! Take a 2-minute break, stretch, and relax your eyes."
                )
            } else {
                AayaApplication.instance.showSystemNotification(
                    title = "💧 Drink Water, $userName",
                    message = "Hey $userName, don't forget to drink a glass of water! Staying hydrated keeps your brain sharp."
                )
            }
        }

        // Schedule next reminder in 2 hours
        scheduleNext(context)
    }

    companion object {
        fun scheduleNext(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, HydrationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9991,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val nextTrigger = System.currentTimeMillis() + (2 * 60 * 60 * 1000L) // 2 hours
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
            }
        }
    }
}
