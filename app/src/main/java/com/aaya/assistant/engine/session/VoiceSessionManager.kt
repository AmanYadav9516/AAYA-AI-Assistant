package com.aaya.assistant.engine.session

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.aaya.assistant.data.local.PreferenceManager
import com.aaya.assistant.engine.audio.TextToSpeechManager
import com.aaya.assistant.engine.audio.TtsCallback
import com.aaya.assistant.engine.router.CommandRouter
import com.aaya.assistant.ui.trigger.VoiceTriggerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class VoiceState {
    SLEEPING,           // Standby / Microphone strictly OFF
    WAKE_DETECTED,      // Wake triggered (Haptic pulse + Launching Translucent Overlay)
    LISTENING_COMMAND,  // Active single-shot command recognition session (5s silence timer)
    PROCESSING,         // Parsing intent and routing
    EXECUTING,          // Performing phone action (Call, SMS, Alarm, Camera, etc.)
    SPEAKING            // AAYA TTS speaking (Microphone strictly OFF)
}

enum class TriggerSource {
    VOICE_WAKE,         // System Assist / Power-Button Hold
    VOLUME_KEYS,        // Volume Up + Down pressed together
    QUICK_SETTINGS,     // Quick Settings Notification Shade Tile
    NOTIFICATION_ACTION,// "Ask AAYA" notification button
    BLUETOOTH_HEADSET,  // Bluetooth earphone / media button
    SHAKE_GESTURE,      // Device shake detection
    IN_APP              // Direct mic button in UI
}

class VoiceSessionManager(
    private val context: Context,
    private val prefs: PreferenceManager,
    private val ttsManager: TextToSpeechManager,
    private val commandRouter: CommandRouter
) : TtsCallback {

    companion object {
        private const val TAG = "AAYA_VOICE"
        private const val SILENCE_TIMEOUT_SECONDS = 5
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // State Flows observed by VoiceTriggerActivity (Siri Floating Overlay) & In-App UI
    private val _voiceState = MutableStateFlow(VoiceState.SLEEPING)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _streamingTranscript = MutableStateFlow("")
    val streamingTranscript: StateFlow<String> = _streamingTranscript.asStateFlow()

    private val _responseText = MutableStateFlow("")
    val responseText: StateFlow<String> = _responseText.asStateFlow()

    private val _actionSummary = MutableStateFlow<String?>(null)
    val actionSummary: StateFlow<String?> = _actionSummary.asStateFlow()

    private val _audioLevelRms = MutableStateFlow(0f)
    val audioLevelRms: StateFlow<Float> = _audioLevelRms.asStateFlow()

    private val _silenceCountdown = MutableStateFlow(SILENCE_TIMEOUT_SECONDS)
    val silenceCountdown: StateFlow<Int> = _silenceCountdown.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var silenceTimerRunnable: Runnable? = null
    private var hasSpeechStarted = false

    init {
        ttsManager.addCallback(this)
    }

    private fun setState(newState: VoiceState) {
        val old = _voiceState.value
        if (old != newState) {
            _voiceState.value = newState
            Log.d(TAG, "state: $old -> $newState")
        }
    }

    /**
     * Unified entry-point for ALL activation methods:
     * Power button hold, Volume Keys, Quick Settings Tile, Notification Action, Shake gesture, In-App.
     * Launches the Siri Translucent Overlay Activity which hosts the floating UI directly.
     */
    fun wakeAaya(source: TriggerSource = TriggerSource.VOICE_WAKE) {
        Log.d(TAG, "wakeAaya triggered by source=$source in state=${_voiceState.value}")

        // 1. Immediate Haptic Pulse
        triggerHapticPulse()

        // 2. Launch Translucent VoiceTriggerActivity (guaranteed foreground Window Token)
        val triggerIntent = Intent(context, VoiceTriggerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TRIGGER_SOURCE", source.name)
        }
        context.startActivity(triggerIntent)
    }

    /**
     * Called by VoiceTriggerActivity.onCreate() or in-app mic button to start active command listening.
     * Single-shot execution with strict 5-second silence sleep timer.
     */
    fun startVoiceSession() {
        Log.d(TAG, "startVoiceSession() initiated")

        // 1. Reset states
        _streamingTranscript.value = ""
        _responseText.value = ""
        _actionSummary.value = null
        _audioLevelRms.value = 0f
        _silenceCountdown.value = SILENCE_TIMEOUT_SECONDS
        hasSpeechStarted = false

        // 2. Request Audio Ducking (drop background music/video volume)
        requestAudioDucking()

        // 3. Set state to LISTENING_COMMAND
        setState(VoiceState.LISTENING_COMMAND)

        // 4. Start strict 5-Second Silence Countdown
        startSilenceCountdown()

        // 5. Initialize SpeechRecognizer on Main Thread
        mainHandler.post {
            try {
                destroyRecognizer()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createCommandListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                }

                speechRecognizer?.startListening(intent)
                Log.d(TAG, "Command recognizer listening started with 5s silence timeout")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start command speech recognizer", e)
                returnToSleep()
            }
        }
    }

    private fun startSilenceCountdown() {
        cancelSilenceCountdown()
        var remainingSeconds = SILENCE_TIMEOUT_SECONDS

        silenceTimerRunnable = object : Runnable {
            override fun run() {
                remainingSeconds--
                _silenceCountdown.value = remainingSeconds
                if (remainingSeconds <= 0) {
                    Log.d(TAG, "5-Second silence timeout reached. User did not speak -> Going back to sleep cleanly.")
                    returnToSleep()
                } else {
                    mainHandler.postDelayed(this, 1000)
                }
            }
        }
        mainHandler.postDelayed(silenceTimerRunnable!!, 1000)
    }

    private fun cancelSilenceCountdown() {
        silenceTimerRunnable?.let { mainHandler.removeCallbacks(it) }
        silenceTimerRunnable = null
    }

    private fun createCommandListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Microphone ready for speech input")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech detected: Cancelling 5s silence countdown")
            hasSpeechStarted = true
            cancelSilenceCountdown()
        }

        override fun onRmsChanged(rmsdB: Float) {
            _audioLevelRms.value = rmsdB
            // If user produces significant audio amplitude, ensure silence countdown is cancelled
            if (rmsdB > 2.5f && !hasSpeechStarted) {
                hasSpeechStarted = true
                cancelSilenceCountdown()
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "Speech ended. Waiting for final results...")
            cancelSilenceCountdown()
        }

        override fun onError(error: Int) {
            Log.w(TAG, "Command recognition error code=$error")
            cancelSilenceCountdown()
            // Single-shot: Never restart or loop microphone on error. Smoothly go to sleep!
            if (_voiceState.value == VoiceState.LISTENING_COMMAND) {
                returnToSleep()
            }
        }

        override fun onResults(results: Bundle?) {
            cancelSilenceCountdown()
            // Turn OFF microphone immediately! Zero background mic usage!
            destroyRecognizer()

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
            val rawCommand = matches.firstOrNull()?.trim() ?: ""
            Log.d(TAG, "Final command recognized: \"$rawCommand\"")

            if (rawCommand.isNotEmpty()) {
                val cleaned = stripWakeWord(rawCommand)
                val finalCommand = if (cleaned.length > 1) cleaned else rawCommand
                processCommand(finalCommand)
            } else {
                returnToSleep()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            cancelSilenceCountdown()
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!partial.isNullOrEmpty()) {
                _streamingTranscript.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun processCommand(command: String) {
        setState(VoiceState.PROCESSING)
        _streamingTranscript.value = "\"$command\""

        scope.launch {
            try {
                setState(VoiceState.EXECUTING)
                val result = commandRouter.routeCommand(command)
                _actionSummary.value = result.actionSummary
                _responseText.value = result.speechResponse

                // Speak response if available
                if (result.speechResponse.isNotBlank()) {
                    speakResponse(result.speechResponse)
                } else {
                    // Action executed without speech (e.g. Flashlight toggled, Screenshot taken)
                    // Keep result visible for 2.2s then return to sleep
                    mainHandler.postDelayed({ returnToSleep() }, 2200)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing command: $command", e)
                _responseText.value = "Error: ${e.localizedMessage}"
                mainHandler.postDelayed({ returnToSleep() }, 2500)
            }
        }
    }

    private fun speakResponse(text: String) {
        setState(VoiceState.SPEAKING)
        destroyRecognizer() // Microphone is strictly OFF while speaking
        ttsManager.speak(text)
    }

    // TtsCallback implementations: strictly prevent self-voice hearing
    override fun onSpeechStarted() {
        Log.d(TAG, "TTS speech started: Mic strictly MUTED")
        destroyRecognizer()
    }

    override fun onSpeechFinished() {
        Log.d(TAG, "TTS speech finished. Smoothly closing overlay and going to sleep.")
        mainHandler.postDelayed({
            returnToSleep()
        }, 1200)
    }

    override fun onSpeechError(error: String) {
        Log.e(TAG, "TTS speech error: $error")
        mainHandler.postDelayed({
            returnToSleep()
        }, 1000)
    }

    /**
     * Completely shuts off microphone, abandons audio ducking, cancels all timers,
     * and sets state to SLEEPING. 0% battery consumption.
     */
    fun returnToSleep() {
        Log.d(TAG, "returnToSleep() executed. Standby mode active.")
        cancelSilenceCountdown()
        destroyRecognizer()
        abandonAudioDucking()
        setState(VoiceState.SLEEPING)
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            speechRecognizer = null
        }
    }

    private fun requestAudioDucking() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .build()
                audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            // Ignore audio focus error
        }
    }

    private fun abandonAudioDucking() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun triggerHapticPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(30)
                }
            }
        } catch (e: Exception) {
            // Ignore haptic failure
        }
    }

    private fun stripWakeWord(text: String): String {
        return text.replace("(?i)(hey aaya|aaya suno|suno aaya|ok aaya|aaya)".toRegex(), "").trim()
    }

    fun onDestroy() {
        ttsManager.removeCallback(this)
        returnToSleep()
    }
}
