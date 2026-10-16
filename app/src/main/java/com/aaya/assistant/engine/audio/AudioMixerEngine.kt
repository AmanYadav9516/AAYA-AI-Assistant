package com.aaya.assistant.engine.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import com.aaya.assistant.AayaApplication

data class EqualizerPreset(
    val name: String,
    val bassBoost: Int, // 0 - 1000
    val virtualizer: Int, // 0 - 1000
    val loudnessGainMb: Int, // 0 - 800
    val bandLevelsPercent: List<Int> // 5 values from -100 to +100
)

class AudioMixerEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AudioMixerEngine"

        @Volatile
        private var instance: AudioMixerEngine? = null

        fun getInstance(context: Context): AudioMixerEngine {
            return instance ?: synchronized(this) {
                instance ?: AudioMixerEngine(context.applicationContext).also { instance = it }
            }
        }

        val PRESETS = listOf(
            EqualizerPreset("Flat / Neutral", 0, 0, 0, listOf(0, 0, 0, 0, 0)),
            EqualizerPreset("Mega Bass Boost 🔥", 850, 400, 300, listOf(80, 50, 0, 20, 40)),
            EqualizerPreset("Vocal & Podcast 🎙️", 100, 200, 200, listOf(-20, 20, 70, 50, 20)),
            EqualizerPreset("Party & Dance 💃", 900, 700, 400, listOf(70, 30, 10, 60, 80)),
            EqualizerPreset("Lo-Fi & Chill ☕", 600, 500, 100, listOf(40, 30, 20, -10, -30)),
            EqualizerPreset("Rock & Metal 🎸", 700, 300, 350, listOf(60, 20, -10, 50, 70))
        )
    }

    private var currentAudioSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    var isEnabled: Boolean = true
        private set

    var currentPresetName: String = "Mega Bass Boost 🔥"
        private set

    var customBassStrength: Int = 750 // 0 to 1000
        private set

    var customVirtualizerStrength: Int = 400 // 0 to 1000
        private set

    var customLoudnessGain: Int = 250 // 0 to 800
        private set

    /**
     * Attaches audio processing effects to the given active MediaPlayer/ExoPlayer audio session.
     */
    fun attachToAudioSession(sessionId: Int) {
        if (sessionId <= 0) return
        if (currentAudioSessionId == sessionId && equalizer != null) return

        releaseEffects()
        currentAudioSessionId = sessionId

        try {
            // 1. Equalizer
            equalizer = Equalizer(0, sessionId).apply {
                enabled = isEnabled
            }

            // 2. Bass Boost
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = isEnabled
                if (strengthSupported) {
                    setStrength(customBassStrength.toShort())
                }
            }

            // 3. Virtualizer (3D Surround)
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = isEnabled
                if (strengthSupported) {
                    setStrength(customVirtualizerStrength.toShort())
                }
            }

            // 4. Loudness Enhancer (API 19+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                        enabled = isEnabled
                        setTargetGain(customLoudnessGain)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "LoudnessEnhancer initialization skipped", e)
                }
            }

            // Apply selected preset
            applyPreset(currentPresetName)
            Log.d(TAG, "Attached AudioMixerEngine to session $sessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach AudioMixerEngine", e)
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        isEnabled = enabled
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        loudnessEnhancer?.enabled = enabled
    }

    fun applyPreset(presetName: String) {
        currentPresetName = presetName
        val preset = PRESETS.firstOrNull { it.name == presetName } ?: return

        setBassBoostStrength(preset.bassBoost)
        setVirtualizerStrength(preset.virtualizer)
        setLoudnessGain(preset.loudnessGainMb)

        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            val minEq = eq.bandLevelRange[0] // e.g. -1500 mB
            val maxEq = eq.bandLevelRange[1] // e.g. +1500 mB
            val totalSpan = maxEq - minEq

            for (i in 0 until numBands) {
                val pct = preset.bandLevelsPercent.getOrNull(i) ?: 0
                // Convert percentage (-100 to 100) to actual millibels
                val level = ((pct / 100f) * (if (pct >= 0) maxEq else -minEq)).toInt()
                eq.setBandLevel(i.toShort(), level.coerceIn(minEq.toInt(), maxEq.toInt()).toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed applying EQ band levels", e)
        }
    }

    fun setBassBoostStrength(strength: Int) {
        customBassStrength = strength.coerceIn(0, 1000)
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(customBassStrength.toShort())
            }
        } catch (_: Exception) {}
    }

    fun setVirtualizerStrength(strength: Int) {
        customVirtualizerStrength = strength.coerceIn(0, 1000)
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(customVirtualizerStrength.toShort())
            }
        } catch (_: Exception) {}
    }

    fun setLoudnessGain(gainMb: Int) {
        customLoudnessGain = gainMb.coerceIn(0, 1000)
        try {
            loudnessEnhancer?.setTargetGain(customLoudnessGain)
        } catch (_: Exception) {}
    }

    fun releaseEffects() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        currentAudioSessionId = 0
    }
}
