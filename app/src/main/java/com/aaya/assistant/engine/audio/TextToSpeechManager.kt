package com.aaya.assistant.engine.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

interface TtsCallback {
    fun onSpeechStarted()
    fun onSpeechFinished()
    fun onSpeechError(error: String)
}

class TextToSpeechManager(
    context: Context,
    private val callback: TtsCallback? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized: Boolean = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.US)
                }
                engine.setPitch(1.05f) // Crisp, modern, energetic tone
                engine.setSpeechRate(1.02f) // Fluid, natural pacing

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        callback?.onSpeechStarted()
                    }

                    override fun onDone(utteranceId: String?) {
                        callback?.onSpeechFinished()
                    }

                    override fun onError(utteranceId: String?) {
                        callback?.onSpeechError("TTS playback error")
                    }
                })
                isInitialized = true
            }
        } else {
            callback?.onSpeechError("TTS Engine initialization failed")
        }
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
            callback?.onSpeechFinished()
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
