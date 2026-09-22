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
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.data.local.PreferenceManager
import com.aaya.assistant.engine.audio.TextToSpeechManager
import com.aaya.assistant.engine.audio.TtsCallback
import com.aaya.assistant.engine.overlay.FloatingOverlayManager
import com.aaya.assistant.engine.router.CommandRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class VoiceState {
    SLEEPING,           // Standby / passive wake-word monitoring
    WAKE_DETECTED,      // Wake triggered (Haptic pulse + Audio ducking)
    LISTENING_COMMAND,  // Active command recognition session
    PROCESSING,         // Parsing intent and routing
    EXECUTING,          // Performing phone action (Call, SMS, Alarm, Camera)
    SPEAKING            // AAYA TTS speaking (Microphone strictly OFF)
}

enum class TriggerSource {
    VOICE_WAKE,         // "Hey AAYA", "AAYA Suno"
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
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val overlayManager = FloatingOverlayManager(context)

    private val _voiceState = MutableStateFlow(VoiceState.SLEEPING)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var commandRetryCount = 0
    private var shouldListenAfterSpeech = false

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
     * Voice, Hardware Volume Keys, Quick Settings Tile, Notification Action, Bluetooth earphone, Shake.
     */
    fun wakeAaya(source: TriggerSource) {
        Log.d(TAG, "wakeAaya triggered by source=$source in state=${_voiceState.value}")

        // 1. Immediate Haptic Pulse
        triggerHapticPulse()

        // 2. Request Audio Ducking (drop background music/video volume)
        requestAudioDucking()

        // 3. Update State
        setState(VoiceState.WAKE_DETECTED)

        // 4. Show Floating Neon Pill Overlay
        val userName = prefs.userName.ifBlank { "there" }
        overlayManager.show("Hello $userName, I'm listening...")

        // 5. If triggered manually (buttons, tile, notification), start command listening immediately
        if (source != TriggerSource.VOICE_WAKE) {
            startCommandListening()
        }
    }

    /**
     * Starts passive wake-word monitoring (Only when screen is ON and wake word enabled).
     */
    fun startPassiveWakeMonitoring() {
        if (_voiceState.value != VoiceState.SLEEPING) return
        if (!powerManager.isInteractive || !prefs.isWakeWordEnabled) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        mainHandler.post {
            try {
                destroyRecognizer()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createPassiveWakeListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                speechRecognizer?.startListening(intent)
                Log.d(TAG, "Passive wake listener started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start passive wake listener", e)
                schedulePassiveWakeRestart(3000)
            }
        }
    }

    /**
     * Stops passive wake monitoring (e.g. when screen turns OFF or when active).
     */
    fun stopPassiveWakeMonitoring() {
        if (_voiceState.value == VoiceState.SLEEPING) {
            destroyRecognizer()
            Log.d(TAG, "Passive wake listener stopped")
        }
    }

    private fun startCommandListening() {
        setState(VoiceState.LISTENING_COMMAND)
        commandRetryCount = 0

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
                    // Generous silence threshold to avoid premature cutoffs
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                }

                speechRecognizer?.startListening(intent)
                overlayManager.updateText("Listening for your command...")
                Log.d(TAG, "Command recognizer started")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting command recognizer", e)
                returnToSleep()
            }
        }
    }

    private fun createPassiveWakeListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            // DO NOT flicker mic rapidly. Restart only with moderate delay when still sleeping
            if (_voiceState.value == VoiceState.SLEEPING && powerManager.isInteractive && prefs.isWakeWordEnabled) {
                schedulePassiveWakeRestart(2000)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
            handlePassiveResults(matches)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Log partials, but NEVER call stopListening() here to preserve full sentence!
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
            val text = matches.firstOrNull() ?: ""
            if (isWakeWordMatch(text)) {
                Log.d(TAG, "Partial wake detected: \"$text\" (Waiting for full utterance...)")
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handlePassiveResults(matches: List<String>) {
        if (matches.isEmpty()) {
            if (_voiceState.value == VoiceState.SLEEPING && powerManager.isInteractive && prefs.isWakeWordEnabled) {
                schedulePassiveWakeRestart(1500)
            }
            return
        }

        val fullSpoken = matches.firstOrNull()?.trim() ?: ""
        Log.d(TAG, "Passive speech received: \"$fullSpoken\"")

        if (isWakeWordMatch(fullSpoken)) {
            // Check for Option A: Single-Breath Utterance ("Hey AAYA make a call to mom")
            val strippedCommand = stripWakeWord(fullSpoken)

            if (strippedCommand.length > 2) {
                // Option A: Command was spoken in the same breath!
                Log.d(TAG, "Option A (Single-Breath) matched: \"$strippedCommand\"")
                wakeAaya(TriggerSource.VOICE_WAKE)
                processCommand(strippedCommand)
            } else {
                // Option B: User only said "Hey AAYA" and stopped -> Greet & listen for follow-up
                Log.d(TAG, "Option B (Two-Stage) matched: Wake only")
                wakeAaya(TriggerSource.VOICE_WAKE)
                speakGreetingAndPrompt()
            }
        } else {
            if (_voiceState.value == VoiceState.SLEEPING && powerManager.isInteractive && prefs.isWakeWordEnabled) {
                schedulePassiveWakeRestart(1200)
            }
        }
    }

    private fun createCommandListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            overlayManager.updateText("Listening...")
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            Log.w(TAG, "Command recognition error code=$error")
            if (_voiceState.value == VoiceState.LISTENING_COMMAND) {
                if (commandRetryCount < 1) {
                    commandRetryCount++
                    overlayManager.updateText("Boliye, sun rahi hoon...")
                    mainHandler.postDelayed({
                        if (_voiceState.value == VoiceState.LISTENING_COMMAND) {
                            startCommandListening()
                        }
                    }, 500)
                } else {
                    val name = prefs.userName.ifBlank { "there" }
                    overlayManager.updateText("Koi aawaz nahi aayi $name. Standby mode.")
                    returnToSleep()
                }
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
            val command = matches.firstOrNull()?.trim() ?: ""
            Log.d(TAG, "Command recognized: \"$command\"")

            if (command.isNotEmpty()) {
                val cleaned = stripWakeWord(command)
                processCommand(if (cleaned.length > 1) cleaned else command)
            } else {
                returnToSleep()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!partial.isNullOrEmpty()) {
                overlayManager.updateText(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun processCommand(command: String) {
        setState(VoiceState.PROCESSING)
        overlayManager.updateText("Working: \"$command\"...")

        scope.launch {
            try {
                setState(VoiceState.EXECUTING)
                val result = commandRouter.routeCommand(command)
                val summary = result.actionSummary
                val speech = result.speechResponse
                val displayText = if (!summary.isNullOrEmpty()) summary else speech
                overlayManager.updateText(displayText)

                // Speak response if available
                if (speech.isNotBlank()) {
                    speakResponse(speech, shouldFollowUp = false)
                } else {
                    mainHandler.postDelayed({ returnToSleep() }, 3000)
                }
            } catch (e: Exception) {
                overlayManager.updateText("Error: ${e.localizedMessage}")
                mainHandler.postDelayed({ returnToSleep() }, 3000)
            }
        }
    }

    private fun speakGreetingAndPrompt() {
        val userName = prefs.userName.ifBlank { "there" }
        val greeting = "Hello $userName, I am here! How can I help you?"
        speakResponse(greeting, shouldFollowUp = true)
    }

    private fun speakResponse(text: String, shouldFollowUp: Boolean) {
        setState(VoiceState.SPEAKING)
        shouldListenAfterSpeech = shouldFollowUp
        destroyRecognizer() // Strictly mute mic while AAYA is speaking
        ttsManager.speak(text)
    }

    // TtsCallback implementations: strictly prevent self-voice hearing
    override fun onSpeechStarted() {
        Log.d(TAG, "TTS speech started: Mic strictly MUTED")
        destroyRecognizer()
    }

    override fun onSpeechFinished() {
        Log.d(TAG, "TTS speech finished. shouldFollowUp=$shouldListenAfterSpeech")
        mainHandler.post {
            if (shouldListenAfterSpeech) {
                shouldListenAfterSpeech = false
                startCommandListening()
            } else {
                mainHandler.postDelayed({ returnToSleep() }, 2000)
            }
        }
    }

    override fun onSpeechError(error: String) {
        Log.e(TAG, "TTS speech error: $error")
        mainHandler.post { returnToSleep() }
    }

    fun returnToSleep() {
        Log.d(TAG, "Returning to SLEEPING state")
        setState(VoiceState.SLEEPING)
        destroyRecognizer()
        abandonAudioDucking()
        overlayManager.hide()

        // Resume passive listening if screen is active
        if (powerManager.isInteractive && prefs.isWakeWordEnabled) {
            schedulePassiveWakeRestart(1000)
        }
    }

    private fun schedulePassiveWakeRestart(delayMs: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (_voiceState.value == VoiceState.SLEEPING && powerManager.isInteractive && prefs.isWakeWordEnabled) {
                startPassiveWakeMonitoring()
            }
        }, delayMs)
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore cleanup
        } finally {
            speechRecognizer = null
        }
    }

    private fun requestAudioDucking() {
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
                    VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(25)
                }
            }
        } catch (e: Exception) {
            // Ignore haptic failure
        }
    }

    private fun isWakeWordMatch(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT).trim()
        return lower.contains("hey aaya") || lower.contains("aaya suno") ||
                lower.contains("suno aaya") || lower.contains("ok aaya") ||
                lower == "aaya" || lower.startsWith("aaya ") || lower.endsWith(" aaya")
    }

    private fun stripWakeWord(text: String): String {
        return text.replace("(?i)(hey aaya|aaya suno|suno aaya|ok aaya|aaya)".toRegex(), "").trim()
    }

    fun onDestroy() {
        ttsManager.removeCallback(this)
        returnToSleep()
    }
}
