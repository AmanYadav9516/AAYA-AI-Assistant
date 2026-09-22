package com.aaya.assistant.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.data.local.PreferenceManager
import com.aaya.assistant.data.model.ApiDiagnostics
import com.aaya.assistant.data.remote.GeminiClient
import com.aaya.assistant.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ApiSettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = AayaApplication.instance.preferenceManager
    val scope = rememberCoroutineScope()
    val geminiClient = remember { GeminiClient(prefs) }

    var apiKeyInput by remember { mutableStateOf(prefs.apiKey.ifEmpty { GeminiClient.DEFAULT_FALLBACK_KEY }) }
    var openRouterKeyInput by remember { mutableStateOf(prefs.openRouterApiKey) }
    var aiProvider by remember { mutableStateOf(prefs.aiProvider) }
    var userNameInput by remember { mutableStateOf(prefs.userName) }
    var assistantNameInput by remember { mutableStateOf(prefs.assistantName) }
    var isShakeEnabled by remember { mutableStateOf(prefs.isShakeEnabled) }
    var shakeSensitivity by remember { mutableFloatStateOf(prefs.shakeSensitivity) }
    var isAutoSleep by remember { mutableStateOf(prefs.isAutoSleepEnabled) }
    var isDrivingAuto by remember { mutableStateOf(prefs.isDrivingModeAuto) }
    var isWaterReminder by remember { mutableStateOf(prefs.isWaterReminderEnabled) }
    var selectedLang by remember { mutableStateOf(prefs.selectedLanguage) }
    var isWakeWord by remember { mutableStateOf(prefs.isWakeWordEnabled) }
    var isVolumeWake by remember { mutableStateOf(prefs.isVolumeWakeEnabled) }
    var isNotifWake by remember { mutableStateOf(prefs.isNotificationWakeEnabled) }
    var isTileWake by remember { mutableStateOf(prefs.isTileWakeEnabled) }
    var isBtWake by remember { mutableStateOf(prefs.isBluetoothWakeEnabled) }
    var isCallAnnounce by remember { mutableStateOf(prefs.isDrivingCallAnnounceEnabled) }
    var isAutoSpeaker by remember { mutableStateOf(prefs.isAutoAnswerSpeakerEnabled) }
    var emergencyPhone by remember { mutableStateOf(prefs.emergencyContactPhone) }
    var emergencyName by remember { mutableStateOf(prefs.emergencyContactName) }

    var showVoiceDialog by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ApiDiagnostics?>(null) }

    if (showVoiceDialog) {
        VoiceCustomizerDialog(
            ttsManager = null,
            onDismiss = { showVoiceDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepIndigoBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "⚙️ Settings & Companion",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Text(
            text = "Personalize your companion, customize voice, and configure AI brain.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // User Profile & Voice Persona Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("User Profile & Voice Persona", fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userNameInput,
                    onValueChange = {
                        userNameInput = it
                        prefs.userName = it
                    },
                    label = { Text("Your Name (e.g. Manish)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Voice Studio Launcher Button
                Button(
                    onClick = { showVoiceDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantPurple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Voice Studio (${prefs.voicePreset})", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API Key Section Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Gemini AI API Configuration", fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // AI Engine Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = aiProvider.uppercase() == "GEMINI",
                        onClick = {
                            aiProvider = "GEMINI"
                            prefs.aiProvider = "GEMINI"
                        },
                        label = { Text("Google Gemini") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RadiantPurple,
                            selectedLabelColor = TextPrimary
                        )
                    )
                    FilterChip(
                        selected = aiProvider.uppercase() == "OPENROUTER",
                        onClick = {
                            aiProvider = "OPENROUTER"
                            prefs.aiProvider = "OPENROUTER"
                        },
                        label = { Text("OpenRouter (Free)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.3f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (aiProvider.uppercase() == "OPENROUTER") {
                    OutlinedTextField(
                        value = openRouterKeyInput,
                        onValueChange = {
                            openRouterKeyInput = it
                            prefs.openRouterApiKey = it
                        },
                        label = { Text("OpenRouter API Key (sk-or-...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextSecondary
                        )
                    )
                } else {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            prefs.apiKey = it
                        },
                        label = { Text("Gemini API Key or Bearer Token") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextSecondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Test Connection Button
                Button(
                    onClick = {
                        scope.launch {
                            isTestingConnection = true
                            testResult = geminiClient.testConnection()
                            isTestingConnection = false
                        }
                    },
                    enabled = !isTestingConnection,
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantPurple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Pinging Google AI Servers…", color = TextPrimary)
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = TextPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Live Connection (Ping)", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                // Live Connection Result Box
                testResult?.let { diag ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (diag.isConnected) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (diag.isConnected) "🟢 Live Connection Verified!" else "🔴 Connection Test Failed",
                                fontWeight = FontWeight.Bold,
                                color = if (diag.isConnected) SuccessGreen else ErrorRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Latency: ${diag.lastLatencyMs}ms | Total Requests Sent: ${diag.totalRequestsSent}", fontSize = 12.sp, color = TextPrimary)
                            if (diag.lastErrorMessage != null) {
                                Text("Error: ${diag.lastErrorMessage}", fontSize = 11.sp, color = ErrorRed)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shake Sensor Configuration Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Shake to Wake AAYA", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Active only when device screen is ON", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                    Switch(
                        checked = isShakeEnabled,
                        onCheckedChange = {
                            isShakeEnabled = it
                            prefs.isShakeEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                if (isShakeEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Shake Sensitivity: ${String.format("%.1f", shakeSensitivity)} m/s²",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Slider(
                        value = shakeSensitivity,
                        onValueChange = {
                            shakeSensitivity = it
                            prefs.shakeSensitivity = it
                        },
                        valueRange = 8.0f..22.0f,
                        steps = 14,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gentle Shake", fontSize = 11.sp, color = TextSecondary)
                        Text("Firm Shake", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Assistant Lifestyle Automation Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Lifestyle Intelligence", fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto Sleep Learning", color = TextPrimary, fontSize = 14.sp)
                        Text("Learns your bedtime and prompts DND automatically", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isAutoSleep,
                        onCheckedChange = {
                            isAutoSleep = it
                            prefs.isAutoSleepEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("💧 Water & Rest Guardian", color = TextPrimary, fontSize = 14.sp)
                        Text("Periodic caring reminders addressing you by name to stay hydrated and rest", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isWaterReminder,
                        onCheckedChange = {
                            isWaterReminder = it
                            prefs.isWaterReminderEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language & Speech Localization Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("🌐 Language & Localization", fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("HINGLISH" to "Hinglish", "HINDI" to "हिंदी (Hindi)", "ENGLISH" to "English").forEach { (langKey, label) ->
                        FilterChip(
                            selected = selectedLang.uppercase() == langKey,
                            onClick = {
                                selectedLang = langKey
                                prefs.selectedLanguage = langKey
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RadiantPurple,
                                selectedLabelColor = TextPrimary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Background Companion & Driving Mode Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("🚗 Background Wake & Driving Mode", fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Voice Wake-Word
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🎙️ Voice Wake (\"Hey AAYA\")", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Listens when screen is ON across WhatsApp, YouTube, and games", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isWakeWord,
                        onCheckedChange = {
                            isWakeWord = it
                            prefs.isWakeWordEnabled = it
                            if (it) {
                                com.aaya.assistant.engine.service.WakeWordForegroundService.start(context)
                            } else {
                                com.aaya.assistant.engine.service.WakeWordForegroundService.stop(context)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                // 2. Volume Up + Down Shortcut
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🔘 Volume Up + Down Shortcut", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Press both volume buttons together to wake AAYA immediately", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isVolumeWake,
                        onCheckedChange = {
                            isVolumeWake = it
                            prefs.isVolumeWakeEnabled = it
                            if (it) {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                // 3. Quick Settings Tile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("⚡ Quick Settings Drawer Tile", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Add 'AAYA Assistant' tile in your phone's notification panel for 1-tap wake", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isTileWake,
                        onCheckedChange = {
                            isTileWake = it
                            prefs.isTileWakeEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                // 4. Notification Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🔔 Notification 'Ask AAYA' Button", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Adds a permanent [🎙️ Ask AAYA] button to the notification bar", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isNotifWake,
                        onCheckedChange = {
                            isNotifWake = it
                            prefs.isNotificationWakeEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                // 5. Bluetooth Headset Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🎧 Bluetooth Earphone Button", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Double-press or hold call/media button on Bluetooth headset to wake AAYA", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isBtWake,
                        onCheckedChange = {
                            isBtWake = it
                            prefs.isBluetoothWakeEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                // Realme / Oppo / Vivo Tip Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark.copy(alpha = 0.6f))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("💡 Realme / Oppo / Vivo User Tip:", fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "ColorOS/Realme UI kills background apps. For 100% reliable wake: Open Settings → Battery → AAYA → Set 'Don't Optimize' and enable 'Auto-launch' in Phone Manager.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Driving: Caller Name Announcer", color = TextPrimary, fontSize = 14.sp)
                        Text("Speaks caller name aloud when phone is ringing", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isCallAnnounce,
                        onCheckedChange = {
                            isCallAnnounce = it
                            prefs.isDrivingCallAnnounceEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto Answer on Speakerphone", color = TextPrimary, fontSize = 14.sp)
                        Text("Automatically answers incoming calls on speaker while driving", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isAutoSpeaker,
                        onCheckedChange = {
                            isAutoSpeaker = it
                            prefs.isAutoAnswerSpeakerEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorder)

                OutlinedTextField(
                    value = emergencyPhone,
                    onValueChange = {
                        emergencyPhone = it
                        prefs.emergencyContactPhone = it
                    },
                    label = { Text("Mummy / Emergency Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    )
                )
            }
        }
    }
}
