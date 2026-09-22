package com.aaya.assistant.engine.wellness

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.ui.theme.NeonCyan
import com.aaya.assistant.ui.theme.RadiantPurple
import com.aaya.assistant.ui.theme.VoidBlack
import java.util.Locale

class MedicineAlertActivity : ComponentActivity() {

    private var medicineName by mutableStateOf("Dawai / Medicine")
    private var reminderText by mutableStateOf("Dawai ka time ho gaya hai! Paani ke sath le lijiye.")
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isConfirmed = false

    private val voiceNagRunnable = object : Runnable {
        override fun run() {
            if (!isConfirmed && !isFinishing) {
                val app = application as? AayaApplication
                val userName = app?.preferenceManager?.userName ?: "Manish"
                app?.ttsManager?.speak("Dawai ka time ho gaya hai $userName, paani ke sath le lijiye.")
                startListeningForConfirmation()
                mainHandler.postDelayed(this, 18000) // Remind every 18s until confirmed
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and show over lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: "Dawai"
        reminderText = "Dawai ka time ho gaya hai ($medicineName)! Paani ke sath le lijiye."

        setContent {
            MedicineAlertContent(
                medicineName = medicineName,
                onConfirm = { confirmAndFinish() },
                onSnooze = { snoozeAndFinish() }
            )
        }

        mainHandler.postDelayed(voiceNagRunnable, 800)
    }

    private fun startListeningForConfirmation() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {}
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                        for (text in matches) {
                            val lower = text.lowercase(Locale.ROOT)
                            if (lower.contains("pee li") || lower.contains("le li") ||
                                lower.contains("taken") || lower.contains("done") || lower.contains("kha li")) {
                                confirmAndFinish()
                                return
                            }
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun confirmAndFinish() {
        if (isConfirmed) return
        isConfirmed = true
        mainHandler.removeCallbacks(voiceNagRunnable)
        speechRecognizer?.destroy()

        val app = application as? AayaApplication
        app?.ttsManager?.speak("Shabash! Health is wealth.")

        mainHandler.postDelayed({
            finish()
        }, 1200)
    }

    private fun snoozeAndFinish() {
        isConfirmed = true
        mainHandler.removeCallbacks(voiceNagRunnable)
        speechRecognizer?.destroy()

        val app = application as? AayaApplication
        app?.ttsManager?.speak("Thik hai, 5 minute baad yaad dilaungi.")
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(voiceNagRunnable)
        speechRecognizer?.destroy()
    }

    companion object {
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"

        fun launch(context: Context, medName: String) {
            val intent = Intent(context, MedicineAlertActivity::class.java).apply {
                putExtra(EXTRA_MEDICINE_NAME, medName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }
}

@Composable
fun MedicineAlertContent(
    medicineName: String,
    onConfirm: () -> Unit,
    onSnooze: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Dawai Ka Time Ho Gaya! 💊",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = medicineName,
                color = NeonCyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Kripya paani ke sath apni dawai le lijiye. AAYA tab tak yaad dilata rahega jab tak aap 'Pee Li' na bolein.",
                color = Color.LightGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VoidBlack)
                Text(
                    text = "  Maine Pee Li (Taken)",
                    color = VoidBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSnooze,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Snooze, contentDescription = null, tint = Color.LightGray)
                Text(
                    text = "  5 Min Baad Yaad Dilana",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }
        }
    }
}
