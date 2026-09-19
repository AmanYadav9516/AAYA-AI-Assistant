package com.aaya.assistant.engine.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.aaya.assistant.AayaApplication

class BatteryReceiver : BroadcastReceiver() {

    private var lastFullNotifiedTime = 0L
    private var lastLowNotifiedTime = 0L

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BATTERY_CHANGED) return

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level == -1 || scale == -1) return

        val batteryPct = (level * 100 / scale.toFloat()).toInt()
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val prefs = AayaApplication.instance.preferenceManager
        val userName = prefs.userName.ifBlank { "there" }
        val now = System.currentTimeMillis()

        // 100% Full Charge Alert (unplug charger)
        if (batteryPct >= 100 && isCharging) {
            // Alert at most once every 30 minutes
            if (now - lastFullNotifiedTime > 30 * 60 * 1000L) {
                lastFullNotifiedTime = now
                val message = "Hey $userName, your phone is 100% charged! Please unplug the charger to protect your battery health."
                AayaApplication.instance.showSystemNotification("🔋 Battery 100% Full", message)
            }
        }

        // Low Battery Alert (<15%)
        if (batteryPct <= 15 && !isCharging) {
            if (now - lastLowNotifiedTime > 45 * 60 * 1000L) {
                lastLowNotifiedTime = now
                val message = "Hey $userName, battery is at $batteryPct%. Please plug in your charger soon."
                AayaApplication.instance.showSystemNotification("⚠️ Battery Low ($batteryPct%)", message)
            }
        }
    }
}
