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

    companion object {
        const val CHANNEL_ASSISTANT_SERVICE = "aaya_service_channel"
        const val CHANNEL_LIFESTYLE_ALERTS = "aaya_lifestyle_alerts"

        lateinit var instance: AayaApplication
            private set
    }
}
