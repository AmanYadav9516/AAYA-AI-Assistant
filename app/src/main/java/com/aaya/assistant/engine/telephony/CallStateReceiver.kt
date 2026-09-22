package com.aaya.assistant.engine.telephony

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import com.aaya.assistant.AayaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallStateReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val app = context.applicationContext as? AayaApplication ?: return
        val prefs = app.preferenceManager
        val tts = app.ttsManager

        if (stateStr == TelephonyManager.EXTRA_STATE_RINGING) {
            if (!prefs.isDrivingCallAnnounceEnabled) return

            val callerName = getContactName(context, incomingNumber) ?: "Unknown Caller"
            val announcement = if (callerName != "Unknown Caller") {
                "$callerName is calling. Say 'Uthao' or press answer."
            } else {
                "Call from an unknown number. Say 'Uthao' to answer."
            }

            Handler(Looper.getMainLooper()).postDelayed({
                tts.speak(announcement)
            }, 800)

            // Auto answer on speakerphone if enabled in settings
            if (prefs.isAutoAnswerSpeakerEnabled) {
                Handler(Looper.getMainLooper()).postDelayed({
                    answerOnSpeakerphone(context)
                }, 2500)
            }
        }
    }

    private fun getContactName(context: Context, phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        return try {
            val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        @SuppressLint("MissingPermission")
        fun answerOnSpeakerphone(context: Context): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    telecomManager.acceptRingingCall()

                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    Handler(Looper.getMainLooper()).postDelayed({
                        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                        audioManager.isSpeakerphoneOn = true
                    }, 500)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}
