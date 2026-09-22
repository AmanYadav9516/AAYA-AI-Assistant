package com.aaya.assistant.engine.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.engine.session.TriggerSource
import kotlin.math.abs

class HardwareKeyAccessibilityService : AccessibilityService() {

    private var lastVolumeUpTime = 0L
    private var lastVolumeDownTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)

        val app = applicationContext as? AayaApplication
        if (app?.preferenceManager?.isVolumeWakeEnabled != true) {
            return super.onKeyEvent(event)
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    lastVolumeUpTime = now
                    if (abs(lastVolumeUpTime - lastVolumeDownTime) <= 350L) {
                        lastVolumeUpTime = 0L
                        lastVolumeDownTime = 0L
                        app.voiceSessionManager.wakeAaya(TriggerSource.VOLUME_KEYS)
                        return true // Consume key event
                    }
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    lastVolumeDownTime = now
                    if (abs(lastVolumeUpTime - lastVolumeDownTime) <= 350L) {
                        lastVolumeUpTime = 0L
                        lastVolumeDownTime = 0L
                        app.voiceSessionManager.wakeAaya(TriggerSource.VOLUME_KEYS)
                        return true // Consume key event
                    }
                }
            }
        }

        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    companion object {
        var instance: HardwareKeyAccessibilityService? = null
            private set

        fun isServiceRunning(): Boolean = instance != null

        fun takeScreenshot(): Boolean {
            val service = instance ?: return false
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else {
                false
            }
        }

        fun pressHome(): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }
}
