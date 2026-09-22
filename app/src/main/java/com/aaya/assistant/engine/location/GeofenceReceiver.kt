package com.aaya.assistant.engine.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.telephony.SmsManager
import com.aaya.assistant.AayaApplication

class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isEntering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)
        if (!isEntering) return

        val phone = intent.getStringExtra(EXTRA_PHONE) ?: return
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Pahuch gaya"

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(phone, null, message, null, null)

            val app = context.applicationContext as? AayaApplication
            app?.showSystemNotification(
                "AAYA Location Commander",
                "Arrived at destination. Auto-sent SMS to $phone: \"$message\""
            )
            app?.ttsManager?.speak("Aap office pahuch gaye hain. Mummy ko message bhej diya hai.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val ACTION_GEOFENCE_TRIGGER = "com.aaya.assistant.GEOFENCE_TRIGGER"
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_MESSAGE = "extra_message"
    }
}
