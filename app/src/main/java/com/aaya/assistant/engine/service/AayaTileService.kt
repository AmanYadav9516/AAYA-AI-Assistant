package com.aaya.assistant.engine.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.engine.session.TriggerSource

@RequiresApi(Build.VERSION_CODES.N)
class AayaTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.label = "AAYA Assistant"
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val app = applicationContext as? AayaApplication ?: return

        // Collapse status bar notification drawer so floating pill is visible
        try {
            @Suppress("DEPRECATION")
            val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeIntent)
        } catch (e: Exception) {
            // Ignore collapse error on newer Android versions
        }

        // Trigger AAYA wake
        app.voiceSessionManager.wakeAaya(TriggerSource.QUICK_SETTINGS)
    }
}
