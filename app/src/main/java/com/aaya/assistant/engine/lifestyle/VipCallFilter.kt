package com.aaya.assistant.engine.lifestyle

import android.content.Context
import com.aaya.assistant.data.local.AayaDatabase
import com.aaya.assistant.engine.router.DeviceController
import java.util.concurrent.ConcurrentHashMap

class VipCallFilter(private val context: Context) {

    private val db = AayaDatabase.getInstance(context)
    private val deviceController = DeviceController(context)

    // Tracks recent calls: NormalizedPhoneNumber -> Timestamp
    private val recentCalls = ConcurrentHashMap<String, Long>()

    suspend fun shouldAllowCallToRing(incomingPhoneNumber: String): Boolean {
        val cleanNumber = incomingPhoneNumber.replace("[^0-9+]".toRegex(), "")

        // 1. VIP Check
        val vip = db.aayaDao().findVipByPhone(cleanNumber)
        if (vip != null && vip.canBypassSleep) {
            // Temporarily un-silence for VIP ring
            deviceController.setDoNotDisturb(false)
            return true
        }

        // 2. Emergency Double-Call rule (called twice within 3 minutes)
        val lastCallTime = recentCalls[cleanNumber]
        val now = System.currentTimeMillis()
        recentCalls[cleanNumber] = now

        if (lastCallTime != null && (now - lastCallTime) < EMERGENCY_WINDOW_MS) {
            // Emergency bypass
            deviceController.setDoNotDisturb(false)
            return true
        }

        return false
    }

    companion object {
        private const val EMERGENCY_WINDOW_MS = 180_000L // 3 minutes
    }
}
