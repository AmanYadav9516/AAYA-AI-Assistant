package com.aaya.assistant.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.engine.audio.TextToSpeechManager
import com.aaya.assistant.ui.theme.*

@Composable
fun VoiceCustomizerDialog(
    ttsManager: TextToSpeechManager?,
    onDismiss: () -> Unit
) {
    val prefs = AayaApplication.instance.preferenceManager

    var selectedPreset by remember { mutableStateOf(prefs.voicePreset) }
    var pitch by remember { mutableFloatStateOf(prefs.voicePitch) }
    var speed by remember { mutableFloatStateOf(prefs.voiceSpeed) }

    val presets = listOf(
        "FEMALE" to "👩 Female (Crisp & Energetic)",
        "MALE" to "👨 Male (Warm & Calm)",
        "CHILD" to "👶 Child (Sweet & Cheerful)",
        "OLD_MAN" to "👴 Old Man (Wise & Slow)",
        "ROBOT" to "🤖 Robot (Sci-Fi Synth)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepIndigoBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Voice Customizer Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "Select an instant AI persona or tune pitch and speed to your liking.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                Text("Character Presets", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { (presetKey, label) ->
                        val isSelected = selectedPreset.equals(presetKey, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) RadiantPurple.copy(alpha = 0.35f) else GlassSurface)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else CardBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedPreset = presetKey
                                    prefs.voicePreset = presetKey
                                    ttsManager?.setVoicePreset(presetKey)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedPreset = presetKey
                                    prefs.voicePreset = presetKey
                                    ttsManager?.setVoicePreset(presetKey)
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Tuning
                Text("Fine-Tune Pitch & Rate", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))

                Text("Pitch: ${String.format("%.2f", pitch)}x", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = pitch,
                    onValueChange = {
                        pitch = it
                        selectedPreset = "CUSTOM"
                        prefs.voicePreset = "CUSTOM"
                        prefs.voicePitch = it
                        ttsManager?.setCustomPitchAndSpeed(pitch, speed)
                    },
                    valueRange = 0.5f..1.8f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )

                Text("Speed / Rate: ${String.format("%.2f", speed)}x", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = speed,
                    onValueChange = {
                        speed = it
                        selectedPreset = "CUSTOM"
                        prefs.voicePreset = "CUSTOM"
                        prefs.voiceSpeed = it
                        ttsManager?.setCustomPitchAndSpeed(pitch, speed)
                    },
                    valueRange = 0.5f..1.8f,
                    colors = SliderDefaults.colors(thumbColor = RadiantPurple, activeTrackColor = RadiantPurple)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Test Voice Button
                Button(
                    onClick = {
                        val name = prefs.userName.ifBlank { "Manish" }
                        ttsManager?.speak("Namaste $name! Main aapki personal AI sahayak AAYA hoon. How can I assist you?")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantPurple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Voice / आवाज़ सुनिए", color = TextPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Done & Save")
                }
            }
        }
    }
}
