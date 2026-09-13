package com.aaya.assistant.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.data.model.AssistantState
import com.aaya.assistant.data.model.MemoryItem
import com.aaya.assistant.data.model.RoutineModel
import com.aaya.assistant.data.model.VipContact
import com.aaya.assistant.data.model.VoiceState
import com.aaya.assistant.data.remote.GeminiClient
import com.aaya.assistant.engine.audio.*
import com.aaya.assistant.engine.contacts.MultilingualContactMatcher
import com.aaya.assistant.engine.router.CommandRouter
import com.aaya.assistant.engine.sensor.ShakeDetectorService
import com.aaya.assistant.ui.assistant.GlowingVoiceOrb
import com.aaya.assistant.ui.assistant.VoiceOverlaySheet
import com.aaya.assistant.ui.memory.MemoryDashboardScreen
import com.aaya.assistant.ui.permissions.PermissionWizardScreen
import com.aaya.assistant.ui.settings.ApiSettingsScreen
import com.aaya.assistant.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var speechRecognizer: StreamingSpeechRecognizer
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var commandRouter: CommandRouter

    private var assistantState by mutableStateOf(AssistantState())
    private var audioLevelRms by mutableFloatStateOf(0f)
    private var showVoiceSheet by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as AayaApplication
        val prefs = app.preferenceManager
        val geminiClient = GeminiClient(prefs)
        val contactMatcher = MultilingualContactMatcher(this)

        ttsManager = TextToSpeechManager(this, object : TtsCallback {
            override fun onSpeechStarted() {
                assistantState = assistantState.copy(voiceState = VoiceState.SPEAKING)
            }

            override fun onSpeechFinished() {
                assistantState = assistantState.copy(voiceState = VoiceState.IDLE)
            }

            override fun onSpeechError(error: String) {
                assistantState = assistantState.copy(voiceState = VoiceState.ERROR)
            }
        })

        speechRecognizer = StreamingSpeechRecognizer(this, object : SpeechRecognitionCallback {
            override fun onReady() {
                assistantState = assistantState.copy(voiceState = VoiceState.LISTENING)
            }

            override fun onAudioLevelChanged(rmsDb: Float) {
                audioLevelRms = rmsDb
            }

            override fun onPartialTranscript(text: String) {
                assistantState = assistantState.copy(streamingTranscript = text)
            }

            override fun onFinalTranscript(text: String, confidence: Float) {
                assistantState = assistantState.copy(
                    voiceState = VoiceState.PROCESSING,
                    streamingTranscript = text,
                    confidenceScore = confidence
                )
                processVoiceCommand(text)
            }

            override fun onError(errorMessage: String) {
                assistantState = assistantState.copy(
                    voiceState = VoiceState.ERROR,
                    finalResponseText = errorMessage
                )
            }
        })

        commandRouter = CommandRouter(this, ttsManager, geminiClient, contactMatcher)

        // Start background shake detector service if enabled
        if (prefs.isShakeEnabled) {
            ShakeDetectorService.start(this)
        }

        handleVoiceTriggerIntent(intent)

        setContent {
            AayaTheme {
                MainAppScaffold(
                    assistantState = assistantState,
                    audioLevelRms = audioLevelRms,
                    showVoiceSheet = showVoiceSheet,
                    onToggleVoiceSession = { toggleVoiceListening() },
                    onStopSpeech = { ttsManager.stop() },
                    onSuggestionClicked = { suggestion ->
                        assistantState = assistantState.copy(streamingTranscript = suggestion)
                        processVoiceCommand(suggestion)
                    },
                    onDismissVoiceSheet = {
                        speechRecognizer.stopListening()
                        ttsManager.stop()
                        showVoiceSheet = false
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleVoiceTriggerIntent(intent)
    }

    private fun handleVoiceTriggerIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_TRIGGER_VOICE, false) == true) {
            showVoiceSheet = true
            startVoiceListening()
        }
    }

    private fun toggleVoiceListening() {
        if (speechRecognizer.isCurrentlyListening()) {
            speechRecognizer.stopListening()
            assistantState = assistantState.copy(voiceState = VoiceState.IDLE)
        } else {
            startVoiceListening()
        }
    }

    private fun startVoiceListening() {
        ttsManager.stop()
        showVoiceSheet = true
        assistantState = assistantState.copy(
            voiceState = VoiceState.LISTENING,
            streamingTranscript = "",
            finalResponseText = "",
            executedActionDescription = ""
        )
        speechRecognizer.startListening()
    }

    private fun processVoiceCommand(commandText: String) {
        if (commandText.isBlank()) return
        val scope = (application as AayaApplication)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            val result = commandRouter.routeAndExecute(commandText)
            assistantState = assistantState.copy(
                finalResponseText = result.speechResponse,
                executedActionDescription = result.actionSummary ?: ""
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.stopListening()
        ttsManager.shutdown()
    }

    companion object {
        const val EXTRA_TRIGGER_VOICE = "extra_trigger_voice"
    }
}

@Composable
fun MainAppScaffold(
    assistantState: AssistantState,
    audioLevelRms: Float,
    showVoiceSheet: Boolean,
    onToggleVoiceSession: () -> Unit,
    onStopSpeech: () -> Unit,
    onSuggestionClicked: (String) -> Unit,
    onDismissVoiceSheet: () -> Unit
) {
    var currentNavIndex by remember { mutableIntStateOf(0) }
    val app = AayaApplication.instance
    val scope = rememberCoroutineScope()

    val memories by app.database.aayaDao().getAllMemory().collectAsState(initial = emptyList())
    val routines by app.database.aayaDao().getAllRoutines().collectAsState(initial = emptyList())
    val vips by app.database.aayaDao().getAllVipContacts().collectAsState(initial = emptyList())

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = NeonCyan
            ) {
                NavigationBarItem(
                    selected = currentNavIndex == 0,
                    onClick = { currentNavIndex = 0 },
                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = "AAYA") },
                    label = { Text("Assistant") }
                )
                NavigationBarItem(
                    selected = currentNavIndex == 1,
                    onClick = { currentNavIndex = 1 },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = "Memory") },
                    label = { Text("Memory") }
                )
                NavigationBarItem(
                    selected = currentNavIndex == 2,
                    onClick = { currentNavIndex = 2 },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Permissions") },
                    label = { Text("Permissions") }
                )
                NavigationBarItem(
                    selected = currentNavIndex == 3,
                    onClick = { currentNavIndex = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepIndigoBg)
        ) {
            when (currentNavIndex) {
                0 -> HomeScreen(
                    isListening = assistantState.voiceState == VoiceState.LISTENING,
                    audioLevelRms = audioLevelRms,
                    onOrbClick = onToggleVoiceSession,
                    onOpenFeature = { query ->
                        onSuggestionClicked(query)
                    }
                )
                1 -> MemoryDashboardScreen(
                    memories = memories,
                    routines = routines,
                    vipContacts = vips,
                    onDeleteMemory = { item -> scope.launch { app.database.aayaDao().deleteMemory(item) } },
                    onToggleRoutine = { routine -> scope.launch { app.database.aayaDao().updateRoutine(routine) } },
                    onDeleteVip = { vip -> scope.launch { app.database.aayaDao().deleteVipContact(vip) } },
                    onAddMemory = { k, v, cat -> scope.launch { app.database.aayaDao().insertMemory(MemoryItem(category = cat, key = k, value = v)) } },
                    onAddVip = { name, phone, rel -> scope.launch { app.database.aayaDao().insertVipContact(VipContact(name = name, phoneNumber = phone, relationship = rel)) } }
                )
                2 -> PermissionWizardScreen(
                    onAllGranted = { currentNavIndex = 0 }
                )
                3 -> ApiSettingsScreen()
            }

            // Floating Assistant Voice Overlay Sheet
            if (showVoiceSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepIndigoBg.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    VoiceOverlaySheet(
                        voiceState = assistantState.voiceState,
                        streamingTranscript = assistantState.streamingTranscript,
                        responseText = assistantState.finalResponseText,
                        actionSummary = assistantState.executedActionDescription,
                        audioLevelRms = audioLevelRms,
                        onOrbClick = onToggleVoiceSession,
                        onStopSpeechClick = onStopSpeech,
                        onSuggestionClick = onSuggestionClicked,
                        onDismiss = onDismissVoiceSheet
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    isListening: Boolean,
    audioLevelRms: Float,
    onOrbClick: () -> Unit,
    onOpenFeature: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "AAYA",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonCyan,
                letterSpacing = 2.sp
            )
            Text(
                text = "Next-Gen Android Voice & Lifestyle Intelligence",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        // Center Animated Orb
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 20.dp)
        ) {
            GlowingVoiceOrb(
                isListening = isListening,
                audioLevelRms = audioLevelRms,
                size = 180.dp,
                onClick = onOrbClick
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = if (isListening) "Listening to you…" else "Tap orb or shake device to speak",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (isListening) NeonCyan else TextPrimary
            )
        }

        // Quick Command Tiles
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Quick Actions", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Flashlight",
                    subtitle = "Toggle Torch",
                    icon = Icons.Default.FlashlightOn,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenFeature("Turn on flashlight") }
                )
                QuickActionCard(
                    title = "Call Mom",
                    subtitle = "Mummy ko call",
                    icon = Icons.Default.Phone,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenFeature("Mummy ko call karo") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Sleep Mode",
                    subtitle = "DND + VIP Ring",
                    icon = Icons.Default.Bedtime,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenFeature("Sleep mode on") }
                )
                QuickActionCard(
                    title = "Smart Alarm",
                    subtitle = "7 Hours Sleep",
                    icon = Icons.Default.Alarm,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenFeature("Set alarm for 7 hours from now") }
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        modifier = modifier
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = null
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text(subtitle, fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}
