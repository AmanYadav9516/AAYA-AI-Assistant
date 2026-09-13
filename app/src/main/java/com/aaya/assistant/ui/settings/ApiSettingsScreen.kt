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
    val prefs = AayaApplication.instance.preferenceManager
    val scope = rememberCoroutineScope()
    val geminiClient = remember { GeminiClient(prefs) }

    var apiKeyInput by remember { mutableStateOf(prefs.apiKey.ifEmpty { GeminiClient.DEFAULT_FALLBACK_KEY }) }
    var assistantNameInput by remember { mutableStateOf(prefs.assistantName) }
    var isShakeEnabled by remember { mutableStateOf(prefs.isShakeEnabled) }
    var shakeSensitivity by remember { mutableFloatStateOf(prefs.shakeSensitivity) }
    var isAutoSleep by remember { mutableStateOf(prefs.isAutoSleepEnabled) }
    var isDrivingAuto by remember { mutableStateOf(prefs.isDrivingModeAuto) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ApiDiagnostics?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepIndigoBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "⚙️ Settings & Diagnostics",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Text(
            text = "Configure your AI Brain, Shake sensitivity, and test live API connectivity.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Driving Mode Auto-Detection", color = TextPrimary, fontSize = 14.sp)
                        Text("Enables hands-free templates during vehicular speed", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isDrivingAuto,
                        onCheckedChange = {
                            isDrivingAuto = it
                            prefs.isDrivingModeAuto = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }
            }
        }
    }
}
