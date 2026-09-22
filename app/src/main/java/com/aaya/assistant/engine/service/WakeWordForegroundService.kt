package com.aaya.assistant.engine.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.R
import com.aaya.assistant.data.local.PreferenceManager
import com.aaya.assistant.engine.audio.TextToSpeechManager
import com.aaya.assistant.engine.overlay.FloatingOverlayManager
import com.aaya.assistant.engine.router.CommandRouter
import com.aaya.assistant.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class WakeWordForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var floatingOverlay: FloatingOverlayManager
    private lateinit var commandRouter: CommandRouter
    private lateinit var powerManager: PowerManager
    private lateinit var audioManager: AudioManager

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isProcessingCommand = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private var audioFocusRequest: AudioFocusRequest? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (preferenceManager.isWakeWordEnabled) {
                        startWakeListening()
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    stopWakeListening()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as AayaApplication
        preferenceManager = app.preferenceManager
        ttsManager = app.ttsManager
        commandRouter = app.commandRouter
        floatingOverlay = FloatingOverlayManager(this)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)

        if (powerManager.isInteractive && preferenceManager.isWakeWordEnabled) {
            startWakeListening()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startWakeListening() {
        if (isListening || isProcessingCommand) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(createWakeListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                isListening = false
                restartListeningWithDelay(2000)
            }
        }
    }

    private fun stopWakeListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
            isListening = false
        }
    }

    private fun createWakeListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            isListening = false
        }

        override fun onError(error: Int) {
            isListening = false
            if (powerManager.isInteractive && preferenceManager.isWakeWordEnabled && !isProcessingCommand) {
                restartListeningWithDelay(1500)
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
            handleDetectedSpeech(matches)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
            for (phrase in matches) {
                if (isWakeWordMatch(phrase)) {
                    speechRecognizer?.stopListening()
                    triggerWakeWordDetected(phrase)
                    break
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun isWakeWordMatch(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT).trim()
        return lower.contains("hey aaya") || lower.contains("aaya suno") ||
                lower.contains("suno aaya") || lower.contains("ok aaya") ||
                lower == "aaya" || lower.startsWith("aaya ") || lower.endsWith(" aaya")
    }

    private fun handleDetectedSpeech(matches: List<String>) {
        if (matches.isEmpty()) {
            if (powerManager.isInteractive && preferenceManager.isWakeWordEnabled && !isProcessingCommand) {
                restartListeningWithDelay(1000)
            }
            return
        }

        for (phrase in matches) {
            if (isWakeWordMatch(phrase)) {
                triggerWakeWordDetected(phrase)
                return
            }
        }

        if (powerManager.isInteractive && preferenceManager.isWakeWordEnabled && !isProcessingCommand) {
            restartListeningWithDelay(1000)
        }
    }

    private fun triggerWakeWordDetected(fullSpokenText: String) {
        if (isProcessingCommand) return
        isProcessingCommand = true
        stopWakeListening()

        // Duck background audio
        requestAudioDucking()

        val userName = preferenceManager.userName.ifBlank { "there" }

        // Strip the wake word to see if user already gave a command in the same breath
        val command = fullSpokenText
            .replace("(?i)(hey aaya|aaya suno|suno aaya|ok aaya|aaya)".toRegex(), "")
            .trim()

        if (floatingOverlay.canDrawOverlays()) {
            floatingOverlay.show("Hello $userName, I'm here! How can I help you?")
        }

        if (command.length > 2) {
            // User gave command directly with wake word (e.g. "Hey AAYA turn on torch")
            processUserCommand(command)
        } else {
            // User only said "Hey AAYA" -> Greeting response & prompt for command
            ttsManager.speak("Hello $userName, I am here! Need any help?")
            mainHandler.postDelayed({
                listenForFollowUpCommand()
            }, 1800)
        }
    }

    private fun listenForFollowUpCommand() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            floatingOverlay.updateText("Listening for your command...")
                        }
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onError(error: Int) {
                            floatingOverlay.updateText("Sorry $userName, I didn't hear anything. Let me know if you need help! 🥺")
                            abandonAudioDucking()
                            isProcessingCommand = false
                            restartListeningWithDelay(2500)
                        }
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                            val cmd = matches.firstOrNull()?.trim() ?: ""
                            if (cmd.isNotEmpty()) {
                                processUserCommand(cmd)
                            } else {
                                abandonAudioDucking()
                                isProcessingCommand = false
                                restartListeningWithDelay(1500)
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                            if (!partial.isNullOrEmpty()) {
                                floatingOverlay.updateText(partial)
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                abandonAudioDucking()
                isProcessingCommand = false
                restartListeningWithDelay(2000)
            }
        }
    }

    private fun processUserCommand(command: String) {
        floatingOverlay.updateText("Working: \"$command\"...")

        serviceScope.launch {
            try {
                val result = commandRouter.routeCommand(command)
                floatingOverlay.updateText(result.actionSummary.ifEmpty { result.speechResponse })
            } catch (e: Exception) {
                floatingOverlay.updateText("Error: ${e.localizedMessage}")
            } finally {
                mainHandler.postDelayed({
                    abandonAudioDucking()
                    isProcessingCommand = false
                    if (powerManager.isInteractive && preferenceManager.isWakeWordEnabled) {
                        startWakeListening()
                    }
                }, 3500)
            }
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
                .build()
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonAudioDucking() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun restartListeningWithDelay(delayMs: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (powerManager.isInteractive && preferenceManager.isWakeWordEnabled && !isProcessingCommand) {
                startWakeListening()
            }
        }, delayMs)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AAYA Background Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps AAYA wake-word listening active while screen is on"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AAYA Voice Companion Active")
            .setContentText("Say \"Hey AAYA\" anytime to speak with your assistant")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenStateReceiver)
        stopWakeListening()
        floatingOverlay.hide()
        abandonAudioDucking()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "aaya_wake_word_channel"
        private const val NOTIFICATION_ID = 2001
        const val ACTION_STOP_SERVICE = "com.aaya.assistant.STOP_WAKE_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, WakeWordForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WakeWordForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
