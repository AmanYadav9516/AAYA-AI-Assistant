package com.aaya.assistant.engine.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.aaya.assistant.data.local.PreferenceManager
import java.util.Locale

interface TtsCallback {
    fun onSpeechStarted()
    fun onSpeechFinished()
    fun onSpeechError(error: String)
}

class TextToSpeechManager(
    private val context: Context,
    var callback: TtsCallback? = null
) : TextToSpeech.OnInitListener {

    private val prefs = PreferenceManager(context)
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized: Boolean = false
    private val callbacks = mutableListOf<TtsCallback>()

    fun addCallback(cb: TtsCallback) {
        if (!callbacks.contains(cb)) callbacks.add(cb)
    }

    fun removeCallback(cb: TtsCallback) {
        callbacks.remove(cb)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.US)
                }

                applySavedVoiceSettings()

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        callback?.onSpeechStarted()
                        callbacks.forEach { it.onSpeechStarted() }
                    }

                    override fun onDone(utteranceId: String?) {
                        callback?.onSpeechFinished()
                        callbacks.forEach { it.onSpeechFinished() }
                    }

                    override fun onError(utteranceId: String?) {
                        callback?.onSpeechError("TTS playback error")
                        callbacks.forEach { it.onSpeechError("TTS playback error") }
                    }
                })
                isInitialized = true
            }
        } else {
            callback?.onSpeechError("TTS Engine initialization failed")
        }
    }

    fun applySavedVoiceSettings() {
        val targetLocale = when (prefs.selectedLanguage.uppercase()) {
            "HINDI" -> Locale("hi", "IN")
            "ENGLISH" -> Locale.US
            else -> Locale.getDefault()
        }
        try {
            tts?.setLanguage(targetLocale)
        } catch (e: Exception) {
            // Fallback
        }

        val preset = prefs.voicePreset
        val (pitch, speed) = when (preset.uppercase()) {
            "FEMALE" -> Pair(1.15f, 1.05f)
            "MALE" -> Pair(0.85f, 0.98f)
            "CHILD" -> Pair(1.40f, 1.10f)
            "OLD_MAN" -> Pair(0.75f, 0.85f)
            "ROBOT" -> Pair(0.55f, 1.15f)
            "CUSTOM" -> Pair(prefs.voicePitch, prefs.voiceSpeed)
            else -> Pair(1.10f, 1.02f)
        }
        setPitchAndSpeed(pitch, speed)
    }

    fun setVoicePreset(preset: String) {
        prefs.voicePreset = preset
        applySavedVoiceSettings()
    }

    fun setCustomPitchAndSpeed(pitch: Float, speed: Float) {
        prefs.voicePreset = "CUSTOM"
        prefs.voicePitch = pitch
        prefs.voiceSpeed = speed
        setPitchAndSpeed(pitch, speed)
    }

    private fun setPitchAndSpeed(pitch: Float, speed: Float) {
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)
    }

    /**
     * Speaks the given text. Immediately cuts off prior speech (barge-in handling).
     */
    fun speak(text: String, utteranceId: String = "aaya_response") {
        if (!isInitialized) return
        stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Immediately stops speaking (Barge-in / Interruption).
     */
    fun stop() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        callback?.onSpeechFinished()
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
