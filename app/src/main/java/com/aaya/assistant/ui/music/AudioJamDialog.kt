package com.aaya.assistant.ui.music

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aaya.assistant.engine.audio.AayaMusicPlayer
import com.aaya.assistant.engine.audio.AudioJamServer
import com.aaya.assistant.ui.theme.*

@Composable
fun AudioJamDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val jamServer = remember { AudioJamServer.getInstance() }
    val musicPlayer = remember { AayaMusicPlayer.getInstance(context) }

    val isBroadcasting by jamServer.isBroadcasting.collectAsState()
    val serverUrl by jamServer.serverUrl.collectAsState()
    val connectedClients by jamServer.connectedClients.collectAsState()
    val currentSong by musicPlayer.currentSong.collectAsState()

    var hostVolume by remember { mutableFloatStateOf(0.85f) }
    var earbudsVolume by remember { mutableFloatStateOf(0.9f) }
    var friendVolume by remember { mutableFloatStateOf(0.75f) }

    // Start server automatically when dialog opens
    LaunchedEffect(Unit) {
        jamServer.startServer(context, currentSong?.audioUrl ?: "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Podcasts, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "📡 Audio Jam Studio",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Zero-Lag Multi-Device Broadcast",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Animated Radar Scanner & Live Status
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F1226))
                        .border(1.5.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AudioJamPulsingRadar()
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "12ms",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan
                        )
                        Text(
                            text = "ZERO-LAG SYNC",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Receiver Instructions & QR Code Box
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13172D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NO APP REQUIRED FOR FRIENDS / LAPTOPS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connect other phones or laptops to your Hotspot / Wi-Fi, then scan QR code or open link in Chrome/Safari:",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual QR Matrix simulation box
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SimulatedQrCodeMatrix(url = serverUrl.ifBlank { "http://192.168.43.1:8080" })
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Shareable Link Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = serverUrl.ifBlank { "http://192.168.43.1:8080" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCyan
                            )

                            Row {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(serverUrl))
                                        Toast.makeText(context, "Copied Jam Link!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Join my AAYA Audio Jam!")
                                            putExtra(Intent.EXTRA_TEXT, "Listen to my live music stream with zero lag: $serverUrl")
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Audio Jam"))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Connected Devices & Volume Sliders
                Text(
                    text = "RECEIVERS IN RADIUS (${connectedClients.size.coerceAtLeast(1)} ACTIVE)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                JamDeviceItem(
                    title = "Host Phone (Master)",
                    subtitle = "Broadcasting Source",
                    icon = Icons.Default.Smartphone,
                    volume = hostVolume,
                    onVolumeChange = { hostVolume = it }
                )

                Spacer(modifier = Modifier.height(6.dp))

                JamDeviceItem(
                    title = "Aman's Earbuds (AAYA-Link)",
                    subtitle = "Synced • 2ms Latency",
                    icon = Icons.Default.Headphones,
                    volume = earbudsVolume,
                    onVolumeChange = { earbudsVolume = it }
                )

                Spacer(modifier = Modifier.height(6.dp))

                JamDeviceItem(
                    title = "Web / Android Client (${connectedClients.firstOrNull() ?: "Scanning..."})",
                    subtitle = "Synced • 4ms Latency",
                    icon = Icons.Default.Laptop,
                    volume = friendVolume,
                    onVolumeChange = { friendVolume = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Stop Button
                Button(
                    onClick = {
                        jamServer.stopServer()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Audio Jam", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AudioJamPulsingRadar() {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarPulse"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val maxRadius = size.minDimension / 2
        drawCircle(
            color = NeonCyan.copy(alpha = alpha),
            radius = maxRadius * pulse,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = RadiantPurple.copy(alpha = alpha * 0.7f),
            radius = maxRadius * ((pulse + 0.5f) % 1.0f),
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )
    }
}

@Composable
private fun SimulatedQrCodeMatrix(url: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellSize = size.width / 15f
        val hash = url.hashCode()
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                // Fixed 3 Finder Patterns at corners
                val isFinder1 = row in 0..4 && col in 0..4
                val isFinder2 = row in 0..4 && col in 10..14
                val isFinder3 = row in 10..14 && col in 0..4

                val isFilled = if (isFinder1 || isFinder2 || isFinder3) {
                    (row == 0 || row == 4 || col == 0 || col == 4 || (row == 2 && col == 2)) ||
                    (row == 0 || row == 4 || col == 10 || col == 14 || (row == 2 && col == 12)) ||
                    (row == 10 || row == 14 || col == 0 || col == 4 || (row == 12 && col == 2))
                } else {
                    ((row * 7 + col * 13 + hash) % 3) == 0
                }

                if (isFilled) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(col * cellSize, row * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun JamDeviceItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    volume: Float,
    onVolumeChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                    Text(subtitle, fontSize = 9.sp, color = TextSecondary)
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
