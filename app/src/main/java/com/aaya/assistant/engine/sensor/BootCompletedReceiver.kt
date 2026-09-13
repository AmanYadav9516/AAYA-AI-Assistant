package com.aaya.assistant.engine.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aaya.assistant.AayaApplication

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON") {
            val prefs = AayaApplication.instance.preferenceManager
            if (prefs.isShakeEnabled) {
                ShakeDetectorService.start(context)
            }
        }
    }
}
