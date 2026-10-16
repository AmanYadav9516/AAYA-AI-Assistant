package com.aaya.assistant.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aaya.assistant.engine.audio.AudioMixerEngine
import com.aaya.assistant.ui.theme.*

@Composable
fun AudioMixerDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val mixer = remember { AudioMixerEngine.getInstance(context) }

    var isMasterEnabled by remember { mutableStateOf(mixer.isEnabled) }
    var selectedPreset by remember { mutableStateOf(mixer.currentPresetName) }
    var bassStrength by remember { mutableFloatStateOf(mixer.customBassStrength / 10f) } // 0 - 100%
    var virtualizerStrength by remember { mutableFloatStateOf(mixer.customVirtualizerStrength / 10f) }
    var loudnessBoost by remember { mutableFloatStateOf(mixer.customLoudnessGain / 8f) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, CardBorder, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
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
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Studio Mixer & EQ", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                            Text("Hardware Bass & Surround DSP", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Switch(
                        checked = isMasterEnabled,
                        onCheckedChange = {
                            isMasterEnabled = it
                            mixer.setMasterEnabled(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets Title
                Text(
                    text = "EQ SOUND PRESETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Presets Horizontal Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AudioMixerEngine.PRESETS) { preset ->
                        val isSelected = selectedPreset == preset.name
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) RadiantPurple else Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NeonCyan else CardBorder.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.clickable {
                                selectedPreset = preset.name
                                mixer.applyPreset(preset.name)
                                bassStrength = preset.bassBoost / 10f
                                virtualizerStrength = preset.virtualizer / 10f
                                loudnessBoost = preset.loudnessGainMb / 8f
                            }
                        ) {
                            Text(
                                text = preset.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sliders Box
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        // 1. Bass Boost
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Subwoofer Bass Boost", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Text("${bassStrength.toInt()}%", fontSize = 12.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = bassStrength,
                            onValueChange = {
                                bassStrength = it
                                mixer.setBassBoostStrength((it * 10).toInt())
                                selectedPreset = "Custom"
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. 3D Spatial Virtualizer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SurroundSound, contentDescription = null, tint = RadiantPurple, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("3D Spatial Surround", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Text("${virtualizerStrength.toInt()}%", fontSize = 12.sp, color = RadiantPurple, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = virtualizerStrength,
                            onValueChange = {
                                virtualizerStrength = it
                                mixer.setVirtualizerStrength((it * 10).toInt())
                                selectedPreset = "Custom"
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = RadiantPurple, activeTrackColor = RadiantPurple)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. Loudness Enhancer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Loudness Volume Gain", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Text("${loudnessBoost.toInt()}%", fontSize = 12.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = loudnessBoost,
                            onValueChange = {
                                loudnessBoost = it
                                mixer.setLoudnessGain((it * 8).toInt())
                                selectedPreset = "Custom"
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply & Close ✨", color = DeepIndigoBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
