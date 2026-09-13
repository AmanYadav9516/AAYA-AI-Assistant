package com.aaya.assistant.ui.assistant

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaya.assistant.data.model.VoiceState
import com.aaya.assistant.ui.theme.*

@Composable
fun VoiceOverlaySheet(
    voiceState: VoiceState,
    streamingTranscript: String,
    responseText: String,
    actionSummary: String?,
    audioLevelRms: Float,
    onOrbClick: () -> Unit,
    onStopSpeechClick: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val quickSuggestions = listOf(
        "Mummy ko call karo",
        "Turn on torch",
        "Open WhatsApp",
        "Sleep mode on",
        "Wake me at 7 AM",
        "Volume up"
    )

    Card(
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Drag Handle & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(TextSecondary.copy(alpha = 0.4f))
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Glowing Animated Voice Orb
            GlowingVoiceOrb(
                isListening = voiceState == VoiceState.LISTENING,
                audioLevelRms = audioLevelRms,
                size = 140.dp,
                onClick = onOrbClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // State status subtitle
            Text(
                text = when (voiceState) {
                    VoiceState.LISTENING -> "Listening to you…"
                    VoiceState.PROCESSING -> "Thinking…"
                    VoiceState.SPEAKING -> "AAYA is speaking…"
                    VoiceState.ERROR -> "Something went wrong"
                    VoiceState.IDLE -> "Tap orb or speak"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = when (voiceState) {
                    VoiceState.LISTENING -> NeonCyan
                    VoiceState.SPEAKING -> RadiantPurple
                    VoiceState.ERROR -> ErrorRed
                    else -> TextSecondary
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real-Time Streaming Transcript Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepIndigoBg.copy(alpha = 0.7f))
                    .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        streamingTranscript.isNotEmpty() -> "\"$streamingTranscript\""
                        responseText.isNotEmpty() -> responseText
                        else -> "Say a command like \"Mummy ko call karo\" or \"Torch on\""
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (streamingTranscript.isNotEmpty()) TextPrimary else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Executed Action Pill
            if (!actionSummary.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ $actionSummary",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan
                    )
                }
            }

            // Barge-In / Stop Button when speaking
            if (voiceState == VoiceState.SPEAKING) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onStopSpeechClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Voice (Barge-in)", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick suggestion pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickSuggestions) { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                            .clickable { onSuggestionClick(suggestion) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(suggestion, fontSize = 12.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}
