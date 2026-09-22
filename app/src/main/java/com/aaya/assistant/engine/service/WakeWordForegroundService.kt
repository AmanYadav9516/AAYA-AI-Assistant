package com.aaya.assistant.engine.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.engine.session.TriggerSource
import com.aaya.assistant.ui.MainActivity

class WakeWordForegroundService : Service() {

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val app = application as? AayaApplication ?: return
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (app.preferenceManager.isWakeWordEnabled) {
                        app.voiceSessionManager.startPassiveWakeMonitoring()
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    app.voiceSessionManager.stopPassiveWakeMonitoring()
                }
                ACTION_WAKE_AAYA -> {
                    app.voiceSessionManager.wakeAaya(TriggerSource.NOTIFICATION_ACTION)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(ACTION_WAKE_AAYA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenStateReceiver, filter)
        }

        val app = application as AayaApplication
        app.voiceSessionManager.startPassiveWakeMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? AayaApplication
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_WAKE_AAYA -> {
                app?.voiceSessionManager?.wakeAaya(TriggerSource.NOTIFICATION_ACTION)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AAYA Voice Companion Standby",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps AAYA voice companion listening for Hey AAYA while screen is on"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action 1: Instant "🎙️ Ask AAYA" Button
        val wakeIntent = Intent(ACTION_WAKE_AAYA).apply {
            setPackage(packageName)
        }
        val wakePending = PendingIntent.getBroadcast(
            this,
            1001,
            wakeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AAYA Voice Companion Active")
            .setContentText("Say \"Hey AAYA\" or tap below to speak")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_btn_speak_now, "🎙️ Ask AAYA", wakePending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            // Ignore unregister
        }
        val app = application as? AayaApplication
        app?.voiceSessionManager?.returnToSleep()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "aaya_wake_word_channel"
        private const val NOTIFICATION_ID = 2001
        const val ACTION_STOP_SERVICE = "com.aaya.assistant.STOP_WAKE_SERVICE"
        const val ACTION_WAKE_AAYA = "com.aaya.assistant.ACTION_WAKE_AAYA"

        fun start(context: Context) {
            val intent = Intent(context, WakeWordForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WakeWordForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
