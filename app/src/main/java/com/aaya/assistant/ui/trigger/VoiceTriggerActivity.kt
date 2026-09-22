package com.aaya.assistant.ui.trigger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.engine.session.TriggerSource
import com.aaya.assistant.ui.MainActivity

class VoiceTriggerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val app = applicationContext as? AayaApplication
            if (Settings.canDrawOverlays(this)) {
                // Instantly summon Siri-style Floating Overlay via VoiceSessionManager
                app?.voiceSessionManager?.wakeAaya(TriggerSource.VOICE_WAKE)
            } else {
                // If overlay permission missing, open MainActivity voice sheet
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_TRIGGER_VOICE, true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(mainIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Finish immediately without animation so foreground app stays active
        finish()
        overridePendingTransition(0, 0)
    }
}
