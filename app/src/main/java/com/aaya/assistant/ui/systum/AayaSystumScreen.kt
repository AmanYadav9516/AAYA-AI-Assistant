package com.aaya.assistant.ui.systum

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaya.assistant.engine.audio.AayaMusicPlayer
import com.aaya.assistant.engine.audio.AudioJamServer
import com.aaya.assistant.engine.briefing.MorningBriefingManager
import com.aaya.assistant.engine.device.DeviceBooster
import com.aaya.assistant.ui.music.AudioJamDialog
import com.aaya.assistant.ui.music.AudioMixerDialog
import com.aaya.assistant.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AayaSystumScreen(
    onOpenTunes: () -> Unit,
    onOpenWallpapers: () -> Unit,
    onSpeakText: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jamServer = remember { AudioJamServer.getInstance() }
    val isJamActive by jamServer.isBroadcasting.collectAsState()

    var showJamDialog by remember { mutableStateOf(false) }
    var showMixerDialog by remember { mutableStateOf(false) }
    var isBoosting by remember { mutableStateOf(false) }

    if (showJamDialog) {
        AudioJamDialog(onDismiss = { showJamDialog = false })
    }

    if (showMixerDialog) {
        AudioMixerDialog(onDismiss = { showMixerDialog = false })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepIndigoBg)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Badge
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AAYA'S SYSTUM",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonCyan.copy(alpha = 0.2f))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = NeonCyan)
                            }
                        }
                        Text(
                            text = "Next-Gen Powered Intelligence & Multimedia Studio",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(RadiantPurple.copy(alpha = 0.25f))
                            .border(1.5.dp, RadiantPurple, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Live Broadcast Banner (Audio Jam Active)
        if (isJamActive) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101935)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, SuccessGreen, RoundedCornerShape(16.dp))
                        .clickable { showJamDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Podcasts, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AUDIO JAM BROADCAST LIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen, letterSpacing = 1.sp)
                            Text("Zero-lag streaming to nearby devices", fontSize = 12.sp, color = TextPrimary)
                        }
                        Text("Manage ➔", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }
                }
            }
        }

        // SECTION 1: Multimedia & Sound Matrix
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "MULTIMEDIA & AUDIO STUDIOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SystumFeatureCard(
                        title = "Audio Jam",
                        subtitle = "Radar & QR WebCast",
                        badge = "Zero-Lag",
                        badgeColor = SuccessGreen,
                        icon = Icons.Default.Podcasts,
                        accentColor = NeonCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { showJamDialog = true }
                    )

                    SystumFeatureCard(
                        title = "AAYA Tunes",
                        subtitle = "Cloud & Local Library",
                        badge = "HQ Audio",
                        badgeColor = RadiantPurple,
                        icon = Icons.Default.LibraryMusic,
                        accentColor = RadiantPurple,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTunes
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SystumFeatureCard(
                        title = "Wallpaper Studio",
                        subtitle = "4K Supabase Collections",
                        badge = "1-Tap Apply",
                        badgeColor = GoldAccent,
                        icon = Icons.Default.Wallpaper,
                        accentColor = GoldAccent,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenWallpapers
                    )

                    SystumFeatureCard(
                        title = "DSP Studio Mixer",
                        subtitle = "5-Band EQ & Bass Boost",
                        badge = "+12dB Boost",
                        badgeColor = ElectricViolet,
                        icon = Icons.Default.GraphicEq,
                        accentColor = ElectricViolet,
                        modifier = Modifier.weight(1f),
                        onClick = { showMixerDialog = true }
                    )
                }
            }
        }

        // SECTION 2: Intelligence & System Optimization
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "INTELLIGENCE & PERFORMANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Device Booster Tile
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                        .clickable {
                            if (!isBoosting) {
                                isBoosting = true
                                scope.launch {
                                    val res = DeviceBooster.optimizeDevice(context)
                                    isBoosting = false
                                    Toast.makeText(context, "🚀 Cleaned ${res.freedMemoryMb} MB RAM!", Toast.LENGTH_SHORT).show()
                                    onSpeakText(res.messageHindi)
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBoosting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NeonCyan, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("1-Tap Performance Booster", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Clean background memory, free RAM & battery diagnostics", fontSize = 11.sp, color = TextSecondary)
                        }
                        Text("Boost ⚡", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Morning Briefing Tile
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                        .clickable {
                            scope.launch {
                                val brief = MorningBriefingManager.generateMorningBriefing(context, isHindi = true)
                                onSpeakText(brief)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Smart Daily Briefing", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Listen to personalized morning news, tasks & inspiration", fontSize = 11.sp, color = TextSecondary)
                        }
                        Text("Listen 🎧", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun SystumFeatureCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        modifier = modifier
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(badge, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = badgeColor)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
