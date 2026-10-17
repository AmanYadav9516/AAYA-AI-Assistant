package com.aaya.assistant.ui.assistant

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaya.assistant.data.model.SongItem
import com.aaya.assistant.engine.audio.AayaMusicPlayer
import com.aaya.assistant.engine.audio.AudioJamServer
import com.aaya.assistant.ui.theme.*

/**
 * Interactive Ambient Dynamic Island Music Pill.
 * Floats unobtrusively when music or Audio Jam is active.
 * 1-tap expands directly to AAYA Tunes or controls playback.
 */
@Composable
fun AmbientMusicPill(
    currentSong: SongItem?,
    isPlaying: Boolean,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentSong == null) return

    val context = LocalContext.current
    val musicPlayer = remember { AayaMusicPlayer.getInstance(context) }
    val jamServer = remember { AudioJamServer.getInstance() }
    val isJamActive by jamServer.isBroadcasting.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 4000 else Int.MAX_VALUE, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAngle"
    )

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF20D1022)),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(NeonCyan, RadiantPurple, ElectricViolet)),
                RoundedCornerShape(32.dp)
            )
            .clickable { onOpenPlayer() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spinning mini vinyl or Jam beacon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161938))
                    .rotate(rotation),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(NeonCyan, Color.Black)))
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track info & Jam indicator
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentSong.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isJamActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SuccessGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("JAM", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                        }
                    }
                }
                Text(
                    text = "${currentSong.artist} • ${if (isPlaying) "Playing" else "Paused"}",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            // Quick Play / Pause Button
            IconButton(
                onClick = { musicPlayer.togglePlayPause() },
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Quick Skip Button
            IconButton(
                onClick = { musicPlayer.playNext() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
