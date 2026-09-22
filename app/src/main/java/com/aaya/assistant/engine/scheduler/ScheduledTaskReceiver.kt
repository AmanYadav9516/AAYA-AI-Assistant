package com.aaya.assistant.engine.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.data.model.AuditLogItem
import com.aaya.assistant.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduledTaskReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", 0L)
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Scheduled Task"
        val taskType = intent.getStringExtra("EXTRA_TASK_TYPE") ?: "CUSTOM_REMINDER"
        val targetData = intent.getStringExtra("EXTRA_TARGET_DATA") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "aaya_scheduled_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AAYA Scheduled Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled calls, study sessions, and tasks"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open app intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        when (taskType) {
            "DND_ON" -> {
                notifBuilder.setContentTitle("🌙 DND Activated")
                    .setContentText("Do Not Disturb mode automatically turned on as scheduled.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                }
                (context.applicationContext as? AayaApplication)?.ttsManager?.speak("DND mode turned on as scheduled.")
            }
            "STOP_MUSIC" -> {
                notifBuilder.setContentTitle("🎵 Music Stopped")
                    .setContentText("Music paused automatically as scheduled.")
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val focusRequest = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).build()
                    audioManager.requestAudioFocus(focusRequest)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.requestAudioFocus(null, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                }
                (context.applicationContext as? AayaApplication)?.ttsManager?.speak("Music playback stopped.")
            }
            "MEDICINE_REMINDER" -> {
                com.aaya.assistant.engine.wellness.MedicineAlertActivity.launch(context, title)
            }
            "CALL_REMINDER" -> {
                notifBuilder.setContentTitle("📞 Call Reminder: $targetData")
                    .setContentText("Time to call $targetData. Tap to open or call directly.")
                if (targetData.isNotBlank()) {
                    val callIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$targetData")
                    }
                    val callPending = PendingIntent.getActivity(
                        context,
                        (taskId + 1000).toInt(),
                        callIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    notifBuilder.addAction(android.R.drawable.ic_menu_call, "Call Now", callPending)
                }
            }
            "STUDY_REMINDER" -> {
                notifBuilder.setContentTitle("📚 Study Session Reminder")
                    .setContentText(title)
            }
            else -> {
                notifBuilder.setContentTitle("🔔 AAYA Reminder")
                    .setContentText(title)
            }
        }

        notificationManager.notify(taskId.toInt(), notifBuilder.build())

        // Mark executed in local database & record in audit log
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AayaApplication.instance.database
                if (taskId > 0L) {
                    db.aayaDao().markTaskExecuted(taskId)
                }
                db.aayaDao().insertAuditLog(
                    AuditLogItem(
                        actionType = "REMINDER",
                        summary = "Triggered scheduled reminder: $title"
                    )
                )
            } catch (e: Exception) {
                // Ignore DB error in background receiver
            }
        }
    }
}
