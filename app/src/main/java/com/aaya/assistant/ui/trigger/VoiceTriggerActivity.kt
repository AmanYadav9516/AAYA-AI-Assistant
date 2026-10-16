package com.aaya.assistant.ui.trigger

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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

                // Auto-close activity when session returns to SLEEPING (unless in interactive Audio Jam Studio)
                LaunchedEffect(voiceState, actionSummary) {
                    if (voiceState == VoiceState.SLEEPING && actionSummary != "Audio Jam Studio") {
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
                    onOpenTunes = {
                        val intent = Intent(this@VoiceTriggerActivity, com.aaya.assistant.ui.MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("OPEN_SCREEN", "AAYA_TUNES")
                        }
                        startActivity(intent)
                        mgr.returnToSleep()
                        finish()
                    },
                    onDismiss = {
                        mgr.returnToSleep()
                        finish()
                        overridePendingTransition(0, 0)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
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
    onOpenTunes: () -> Unit,
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
                onOpenTunes = onOpenTunes,
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
    onOpenTunes: () -> Unit,
    onCloseClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SiriAurora")
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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
                .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
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
                            when {
                                actionSummary == "Audio Jam Studio" -> NeonCyan.copy(alpha = 0.2f)
                                voiceState == VoiceState.LISTENING_COMMAND -> NeonCyan.copy(alpha = 0.15f)
                                voiceState == VoiceState.PROCESSING || voiceState == VoiceState.EXECUTING -> ElectricViolet.copy(alpha = 0.2f)
                                voiceState == VoiceState.SPEAKING -> RadiantPurple.copy(alpha = 0.2f)
                                else -> CardBorder.copy(alpha = 0.3f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            actionSummary == "Audio Jam Studio" -> "📡 Audio Jam Studio"
                            voiceState == VoiceState.LISTENING_COMMAND -> "🎙️ Listening ($silenceCountdown s)"
                            voiceState == VoiceState.PROCESSING -> "⚡ Thinking..."
                            voiceState == VoiceState.EXECUTING -> "⚙️ Working..."
                            voiceState == VoiceState.SPEAKING -> "🔊 Speaking..."
                            voiceState == VoiceState.WAKE_DETECTED -> "✨ Awake"
                            else -> "💤 Standby"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            actionSummary == "Audio Jam Studio" -> NeonCyan
                            voiceState == VoiceState.LISTENING_COMMAND -> NeonCyan
                            voiceState == VoiceState.PROCESSING || voiceState == VoiceState.EXECUTING -> ElectricViolet
                            voiceState == VoiceState.SPEAKING -> RadiantPurple
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

            // Render Audio Jam Studio if active
            if (actionSummary == "Audio Jam Studio") {
                AudioJamStudioView(
                    onOpenTunes = onOpenTunes,
                    onStopJam = onCloseClick
                )
            } else {
                // Dynamic Island Expansion for Detailed Gemini Answers vs Standard Message
                val displayMessage = when {
                    streamingTranscript.isNotEmpty() -> streamingTranscript
                    responseText.isNotEmpty() -> responseText
                    voiceState == VoiceState.LISTENING_COMMAND -> "Say a command (e.g. \"Call Mom\", \"Torch on\")..."
                    voiceState == VoiceState.PROCESSING -> "Processing your request..."
                    voiceState == VoiceState.EXECUTING -> "Executing..."
                    else -> "AAYA is ready."
                }

                val isLongAnswer = responseText.isNotEmpty() && (responseText.length > 90 || responseText.contains("\n"))

                if (isLongAnswer) {
                    // Full expanded card with vertical scroll for comprehensive Gemini answers
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = responseText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Gemini AI Knowledge",
                            fontSize = 11.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(responseText))
                                Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Answer", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        }
                    }
                } else {
                    Text(
                        text = displayMessage,
                        fontSize = if (streamingTranscript.isNotEmpty()) 17.sp else 15.sp,
                        fontWeight = if (streamingTranscript.isNotEmpty()) FontWeight.Bold else FontWeight.Medium,
                        color = if (streamingTranscript.isNotEmpty()) TextPrimary else TextSecondary,
                        maxLines = if (streamingTranscript.isNotEmpty()) 3 else 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }

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
}

/**
 * Interactive Audio Jam Studio embedded within the expanded lighting card
 */
@Composable
private fun AudioJamStudioView(
    onOpenTunes: () -> Unit,
    onStopJam: () -> Unit
) {
    var hostVolume by remember { mutableFloatStateOf(0.85f) }
    var earbudsVolume by remember { mutableFloatStateOf(0.9f) }
    var friendPhoneVolume by remember { mutableFloatStateOf(0.75f) }
    var isMutedAll by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        // Radar Scanner & Network Status Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131828)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated Pulsing Radar Beacon
                AudioJamRadarBeacon()

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AAYA-JAM-SYNC",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SuccessGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("12ms ZERO-LAG", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hotspot Broadcasting • Connect nearby devices to this Wi-Fi",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "CONNECTED JAM RECEIVERS (3 IN RADIUS)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Device 1: Host Phone
        JamDeviceVolumeRow(
            deviceName = "Host Device (This Phone)",
            deviceTypeIcon = Icons.Default.Smartphone,
            status = "Master Audio Source",
            volume = if (isMutedAll) 0f else hostVolume,
            onVolumeChange = { hostVolume = it }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Device 2: Earbuds
        JamDeviceVolumeRow(
            deviceName = "Aman's Earbuds (AAYA-Link)",
            deviceTypeIcon = Icons.Default.Headphones,
            status = "Synced • 2ms Latency",
            volume = if (isMutedAll) 0f else earbudsVolume,
            onVolumeChange = { earbudsVolume = it }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Device 3: Friend Phone
        JamDeviceVolumeRow(
            deviceName = "Friend's Android (Redmi Note)",
            deviceTypeIcon = Icons.Default.Smartphone,
            status = "Synced • 4ms Latency",
            volume = if (isMutedAll) 0f else friendPhoneVolume,
            onVolumeChange = { friendPhoneVolume = it }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenTunes,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = DeepIndigoBg, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Tunes", color = DeepIndigoBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onStopJam,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stop Jam", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Animated Pulsing Radar Beacon for Audio Jam radius search
 */
@Composable
private fun AudioJamRadarBeacon() {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        // Expanding ring
        Box(
            modifier = Modifier
                .size((36 * pulseScale).dp)
                .clip(CircleShape)
                .border(2.dp, NeonCyan.copy(alpha = pulseAlpha), CircleShape)
        )
        // Center core
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(NeonCyan)
        )
    }
}

/**
 * Individual volume slider row for each connected device in the Jam
 */
@Composable
private fun JamDeviceVolumeRow(
    deviceName: String,
    deviceTypeIcon: ImageVector,
    status: String,
    volume: Float,
    onVolumeChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(deviceTypeIcon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(deviceName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                    Text(status, fontSize = 9.sp, color = TextSecondary)
                }
                Text("${(volume * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            }
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
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
