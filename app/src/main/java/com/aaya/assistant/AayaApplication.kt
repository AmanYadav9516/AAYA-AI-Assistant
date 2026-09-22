package com.aaya.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.aaya.assistant.data.local.AayaDatabase
import com.aaya.assistant.data.local.PreferenceManager

class AayaApplication : Application() {

    lateinit var database: AayaDatabase
        private set

    lateinit var preferenceManager: PreferenceManager
        private set

    lateinit var ttsManager: com.aaya.assistant.engine.audio.TextToSpeechManager
        private set

    lateinit var geminiClient: com.aaya.assistant.data.remote.GeminiClient
        private set

    lateinit var contactMatcher: com.aaya.assistant.engine.contacts.MultilingualContactMatcher
        private set

    lateinit var commandRouter: com.aaya.assistant.engine.router.CommandRouter
        private set

    lateinit var voiceSessionManager: com.aaya.assistant.engine.session.VoiceSessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            preferenceManager = PreferenceManager(this)
        } catch (t: Throwable) {
            // Safeguarded
        }

        try {
            database = AayaDatabase.getInstance(this)
        } catch (t: Throwable) {
            // Safeguarded
        }

        try {
            ttsManager = com.aaya.assistant.engine.audio.TextToSpeechManager(this)
            geminiClient = com.aaya.assistant.data.remote.GeminiClient(preferenceManager)
            contactMatcher = com.aaya.assistant.engine.contacts.MultilingualContactMatcher(this)
            commandRouter = com.aaya.assistant.engine.router.CommandRouter(this, ttsManager, geminiClient, contactMatcher)
            voiceSessionManager = com.aaya.assistant.engine.session.VoiceSessionManager(this, preferenceManager, ttsManager, commandRouter)
        } catch (t: Throwable) {
            // Safeguarded
        }

        try {
            createNotificationChannels()
        } catch (t: Throwable) {
            // Safeguarded
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val assistantChannel = NotificationChannel(
                CHANNEL_ASSISTANT_SERVICE,
                "AAYA Assistant Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when AAYA background listener or shake detection is active"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_LIFESTYLE_ALERTS,
                "AAYA Lifestyle & Briefings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Delivers sleep prompts, class mode reminders, and morning briefings"
            }

            notificationManager.createNotificationChannel(assistantChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    fun showSystemNotification(title: String, message: String) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val openAppIntent = android.content.Intent(this, com.aaya.assistant.ui.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                openAppIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notif = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_LIFESTYLE_ALERTS)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notif)
        } catch (e: Exception) {
            // Ignore notification failure
        }
    }

    companion object {
        const val CHANNEL_ASSISTANT_SERVICE = "aaya_service_channel"
        const val CHANNEL_LIFESTYLE_ALERTS = "aaya_lifestyle_alerts"

        lateinit var instance: AayaApplication
            private set
    }
}
