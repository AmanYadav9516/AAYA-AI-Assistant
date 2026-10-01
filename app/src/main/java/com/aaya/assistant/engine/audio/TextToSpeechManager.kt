package com.aaya.assistant.engine.audio

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
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
                // Prefer Indian bilingual voice by default for natural Hindi/Hinglish/English articulation
                val preferredLocale = when (prefs.selectedLanguage.uppercase(Locale.ROOT)) {
                    "HINDI" -> Locale("hi", "IN")
                    "ENGLISH" -> Locale("en", "IN")
                    else -> Locale("en", "IN") // Hinglish thrives on Indian English phonetics
                }

                val result = engine.setLanguage(preferredLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val fallback = engine.setLanguage(Locale("hi", "IN"))
                    if (fallback == TextToSpeech.LANG_MISSING_DATA || fallback == TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.setLanguage(Locale.US)
                    }
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
        val engine = tts ?: return
        val targetLocale = when (prefs.selectedLanguage.uppercase(Locale.ROOT)) {
            "HINDI" -> Locale("hi", "IN")
            "ENGLISH" -> Locale("en", "IN")
            else -> Locale("en", "IN") // Indian English handles Romanized Hindi / Hinglish naturally
        }

        try {
            engine.setLanguage(targetLocale)
        } catch (_: Exception) {
            // Ignore
        }

        val preset = prefs.voicePreset.uppercase(Locale.ROOT)
        val (pitch, speed) = when (preset) {
            "FEMALE" -> Pair(1.08f, 1.02f)
            "MALE" -> Pair(0.85f, 0.95f)
            "CHILD" -> Pair(1.35f, 1.05f)
            "OLD_MAN" -> Pair(0.72f, 0.85f)
            "ROBOT" -> Pair(0.55f, 1.12f)
            "CUSTOM" -> Pair(prefs.voicePitch, prefs.voiceSpeed)
            else -> Pair(1.05f, 1.0f)
        }
        setPitchAndSpeed(pitch, speed)

        // Attempt actual neural voice selection if supported
        try {
            val availableVoices = engine.voices
            if (!availableVoices.isNullOrEmpty()) {
                val matchedVoice = when (preset) {
                    "FEMALE" -> availableVoices.firstOrNull { v ->
                        val n = v.name.lowercase(Locale.ROOT)
                        (v.locale.country == "IND" || v.locale.language in listOf("hi", "en")) &&
                                (n.contains("female") || n.contains("hie") || n.contains("end") || n.contains("network"))
                    }
                    "MALE" -> availableVoices.firstOrNull { v ->
                        val n = v.name.lowercase(Locale.ROOT)
                        (v.locale.country == "IND" || v.locale.language in listOf("hi", "en")) &&
                                (n.contains("male") || n.contains("hic") || n.contains("enc"))
                    }
                    else -> availableVoices.firstOrNull { v ->
                        v.locale.country == "IND" || v.locale.language in listOf("hi", "en")
                    }
                }
                if (matchedVoice != null) {
                    engine.voice = matchedVoice
                }
            }
        } catch (_: Exception) {
            // Fallback to pitch modulation
        }
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
     * Sanitizes response text and normalizes pronunciation so AAYA is spoken as "Aaya" rather
     * than letter-by-letter acronym "A - A - Y - A", and removes unwanted Markdown syntax.
     */
    private fun sanitizeForSpeech(raw: String): String {
        var clean = raw
            // Replace all-caps AAYA with natural phonetic word "Aaya"
            .replace(Regex("(?i)\\bAAYA\\b"), "Aaya")
            .replace(Regex("(?i)\\bA A Y A\\b"), "Aaya")
            // Strip markdown formatting symbols that TTS engines read literally
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^[-*•]\\s*", RegexOption.MULTILINE), "")
            // Normalize multiple whitespaces
            .replace(Regex("\\s+"), " ")
            .trim()

        // If speaking in Hindi mode, guide phonetics
        if (prefs.selectedLanguage.equals("HINDI", ignoreCase = true)) {
            clean = clean.replace(Regex("(?i)\\bAaya\\b"), "आया")
        }

        return clean
    }

    /**
     * Speaks the given text with natural pronunciation and barge-in cut-off.
     */
    fun speak(text: String, utteranceId: String = "aaya_response") {
        if (!isInitialized) return
        stop()
        val speechReadyText = sanitizeForSpeech(text)
        if (speechReadyText.isBlank()) return
        tts?.speak(speechReadyText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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
