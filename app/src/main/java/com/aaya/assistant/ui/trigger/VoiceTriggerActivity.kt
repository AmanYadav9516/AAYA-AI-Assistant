package com.aaya.assistant.ui.trigger

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.engine.session.VoiceSessionManager
import com.aaya.assistant.engine.session.VoiceState
import com.aaya.assistant.ui.theme.*
import kotlinx.coroutines.delay

class VoiceTriggerActivity : ComponentActivity() {

    private var sessionManager: VoiceSessionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make window fully transparent and keep screen awake while listening
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val app = applicationContext as? AayaApplication
        sessionManager = app?.voiceSessionManager

        if (sessionManager == null) {
            finish()
            overridePendingTransition(0, 0)
            return
        }

        // Start single-shot listening session with strict 5-second silence timer
        sessionManager?.startVoiceSession()

        setContent {
            AayaTheme {
                val mgr = sessionManager ?: return@AayaTheme

                val voiceState by mgr.voiceState.collectAsState()
                val streamingTranscript by mgr.streamingTranscript.collectAsState()
                val responseText by mgr.responseText.collectAsState()
                val actionSummary by mgr.actionSummary.collectAsState()
                val audioLevelRms by mgr.audioLevelRms.collectAsState()
                val silenceCountdown by mgr.silenceCountdown.collectAsState()

                // Intercept back button to dismiss cleanly
                BackHandler {
                    mgr.returnToSleep()
                    finish()
                    overridePendingTransition(0, 0)
                }

                // Auto-close activity when session returns to SLEEPING
                LaunchedEffect(voiceState) {
                    if (voiceState == VoiceState.SLEEPING) {
                        delay(250)
                        finish()
                        overridePendingTransition(0, 0)
                    }
                }

                SiriTranslucentScreen(
                    voiceState = voiceState,
                    streamingTranscript = streamingTranscript,
                    responseText = responseText,
                    actionSummary = actionSummary,
                    audioLevelRms = audioLevelRms,
                    silenceCountdown = silenceCountdown,
                    onDismiss = {
                        mgr.returnToSleep()
                        finish()
                        overridePendingTransition(0, 0)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // User triggered again: start a fresh listening turn
        sessionManager?.startVoiceSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (sessionManager?.voiceState?.value != VoiceState.SLEEPING) {
            sessionManager?.returnToSleep()
        }
    }
}

@Composable
private fun SiriTranslucentScreen(
    voiceState: VoiceState,
    streamingTranscript: String,
    responseText: String,
    actionSummary: String?,
    audioLevelRms: Float,
    silenceCountdown: Int,
    onDismiss: () -> Unit
) {
    // Semi-transparent backdrop: tapping anywhere outside dismisses AAYA
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // Floating Siri Island Pill at Top Center
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Consume clicks inside card so it doesn't dismiss */ }
                )
        ) {
            SiriFloatingCard(
                voiceState = voiceState,
                streamingTranscript = streamingTranscript,
                responseText = responseText,
                actionSummary = actionSummary,
                audioLevelRms = audioLevelRms,
                silenceCountdown = silenceCountdown,
                onCloseClick = onDismiss
            )
        }
    }
}

@Composable
private fun SiriFloatingCard(
    voiceState: VoiceState,
    streamingTranscript: String,
    responseText: String,
    actionSummary: String?,
    audioLevelRms: Float,
    silenceCountdown: Int,
    onCloseClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SiriAurora")

    // Continuous Aurora Gradient Border Rotation
    val gradientAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuroraAngle"
    )

    val auroraBrush = remember(gradientAngle) {
        Brush.sweepGradient(
            colors = listOf(
                Color(0xFF00E5FF), // Neon Cyan
                Color(0xFF7C4DFF), // Electric Violet
                Color(0xFFFF2A85), // Hot Magenta
                Color(0xFF00E5FF)  // Neon Cyan
            )
        )
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF20B0E17)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp), spotColor = NeonCyan)
            .border(2.5.dp, auroraBrush, RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth()
        ) {
            // Header Row: Soundwave Bars, Status Badge, Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated 5-Bar Dancing Soundwave
                SiriDancingSoundwave(
                    isListening = voiceState == VoiceState.LISTENING_COMMAND,
                    isSpeaking = voiceState == VoiceState.SPEAKING,
                    audioLevelRms = audioLevelRms
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Dynamic Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (voiceState) {
                                VoiceState.LISTENING_COMMAND -> NeonCyan.copy(alpha = 0.15f)
                                VoiceState.PROCESSING, VoiceState.EXECUTING -> ElectricViolet.copy(alpha = 0.2f)
                                VoiceState.SPEAKING -> RadiantPurple.copy(alpha = 0.2f)
                                else -> CardBorder.copy(alpha = 0.3f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (voiceState) {
                            VoiceState.LISTENING_COMMAND -> "🎙️ Listening ($silenceCountdown s)"
                            VoiceState.PROCESSING -> "⚡ Thinking..."
                            VoiceState.EXECUTING -> "⚙️ Working..."
                            VoiceState.SPEAKING -> "🔊 Speaking..."
                            VoiceState.WAKE_DETECTED -> "✨ Awake"
                            VoiceState.SLEEPING -> "💤 Standby"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (voiceState) {
                            VoiceState.LISTENING_COMMAND -> NeonCyan
                            VoiceState.PROCESSING, VoiceState.EXECUTING -> ElectricViolet
                            VoiceState.SPEAKING -> RadiantPurple
                            else -> TextSecondary
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Sleek Close "✕" Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { onCloseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Transcript / Message Display
            val displayMessage = when {
                streamingTranscript.isNotEmpty() -> streamingTranscript
                responseText.isNotEmpty() -> responseText
                voiceState == VoiceState.LISTENING_COMMAND -> "Say a command (e.g. \"Call Mom\", \"Torch on\")..."
                voiceState == VoiceState.PROCESSING -> "Processing your request..."
                voiceState == VoiceState.EXECUTING -> "Executing..."
                else -> "AAYA is ready."
            }

            Text(
                text = displayMessage,
                fontSize = if (streamingTranscript.isNotEmpty()) 17.sp else 15.sp,
                fontWeight = if (streamingTranscript.isNotEmpty()) FontWeight.Bold else FontWeight.Medium,
                color = if (streamingTranscript.isNotEmpty()) TextPrimary else TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Executed Action Pill (e.g. "⚡ Calling Mummy", "⚡ Torch ON")
            if (!actionSummary.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(NeonCyan.copy(alpha = 0.18f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚡ $actionSummary",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }

            // Silence Countdown Progress Bar (Only during listening)
            if (voiceState == VoiceState.LISTENING_COMMAND) {
                Spacer(modifier = Modifier.height(14.dp))
                val animatedProgress by animateFloatAsState(
                    targetValue = (silenceCountdown / 5f).coerceIn(0f, 1f),
                    animationSpec = tween(900, easing = LinearEasing),
                    label = "SilenceTimerBar"
                )

                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = NeonCyan,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
            }
        }
    }
}

/**
 * 5-Bar Siri Audio Waveform Visualizer:
 * Bar heights dance dynamically in response to speech volume (audioLevelRms)
 */
@Composable
private fun SiriDancingSoundwave(
    isListening: Boolean,
    isSpeaking: Boolean,
    audioLevelRms: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    val colors = listOf(
        Color(0xFF00E5FF), // Cyan
        Color(0xFF7C4DFF), // Purple
        Color.White,       // Bright Core
        Color(0xFFFF2A85), // Magenta
        Color(0xFF00E5FF)  // Cyan
    )

    Canvas(modifier = Modifier.size(width = 38.dp, height = 24.dp)) {
        val barCount = 5
        val barWidth = 4.dp.toPx()
        val spacing = (size.width - (barCount * barWidth)) / (barCount - 1)
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val baseSine = kotlin.math.sin(phase + (i * 1.2f))
            val factor = if (isListening) {
                val rmsFactor = (audioLevelRms.coerceIn(0f, 12f) / 12f)
                0.25f + (rmsFactor * 0.75f) * (0.6f + 0.4f * kotlin.math.abs(baseSine.toFloat()))
            } else if (isSpeaking) {
                0.3f + 0.6f * kotlin.math.abs(baseSine.toFloat())
            } else {
                0.2f // Subtle standby height
            }

            val barHeight = (maxHeight * factor).coerceIn(4.dp.toPx(), maxHeight)
            val left = i * (barWidth + spacing)
            val top = (maxHeight - barHeight) / 2

            drawRoundRect(
                color = colors[i % colors.size],
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
