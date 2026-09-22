package com.aaya.assistant.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.data.model.*
import com.aaya.assistant.data.remote.GeminiClient
import com.aaya.assistant.engine.audio.*
import com.aaya.assistant.data.local.QuoteLibrary
import com.aaya.assistant.engine.contacts.MultilingualContactMatcher
import com.aaya.assistant.engine.festival.FestivalManager
import com.aaya.assistant.engine.router.CommandRouter
import com.aaya.assistant.engine.sensor.ShakeDetectorService
import com.aaya.assistant.engine.wellness.HydrationReceiver
import com.aaya.assistant.ui.assistant.GlowingVoiceOrb
import com.aaya.assistant.ui.assistant.VoiceOverlaySheet
import com.aaya.assistant.ui.festival.FestivalGreetingDialog
import com.aaya.assistant.ui.memory.MemoryDashboardScreen
import com.aaya.assistant.ui.notes.NotesAndScheduleScreen
import com.aaya.assistant.ui.permissions.PermissionWizardScreen
import com.aaya.assistant.ui.settings.ApiSettingsScreen
import com.aaya.assistant.ui.settings.VoiceCustomizerDialog
import com.aaya.assistant.ui.theme.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
        try {
            if (prefs.isShakeEnabled) {
                ShakeDetectorService.start(this)
            }
        } catch (t: Throwable) {
            // Safeguarded against Android 14 FGS restrictions
        }

        // Start background wake-word listening service if enabled
        try {
            if (prefs.isWakeWordEnabled) {
                com.aaya.assistant.engine.service.WakeWordForegroundService.start(this)
            }
        } catch (t: Throwable) {
            // Safeguarded
        }

        // Schedule proactive hydration & rest guardian reminders
        try {
            if (prefs.isWaterReminderEnabled) {
                HydrationReceiver.scheduleNext(this)
            }
        } catch (t: Throwable) {
            // Ignore
        }

        handleVoiceTriggerIntent(intent)

        setContent {
            AayaTheme {
                MainAppScaffold(
                    assistantState = assistantState,
                    audioLevelRms = audioLevelRms,
                    showVoiceSheet = showVoiceSheet,
                    ttsManager = ttsManager,
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
        lifecycleScope.launch {
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
    ttsManager: TextToSpeechManager,
    onToggleVoiceSession: () -> Unit,
    onStopSpeech: () -> Unit,
    onSuggestionClicked: (String) -> Unit,
    onDismissVoiceSheet: () -> Unit
) {
    val app = AayaApplication.instance
    val initialNav = if (!app.preferenceManager.isInitialSetupDone) 3 else 0
    var currentNavIndex by remember { mutableIntStateOf(initialNav) }
    val scope = rememberCoroutineScope()

    var showVoiceCustomizer by remember { mutableStateOf(false) }
    var selectedFestivalForDialog by remember { mutableStateOf<com.aaya.assistant.data.model.FestivalModel?>(null) }

    val memories by app.database.aayaDao().getAllMemory().collectAsState(initial = emptyList())
    val routines by app.database.aayaDao().getAllRoutines().collectAsState(initial = emptyList())
    val vips by app.database.aayaDao().getAllVipContacts().collectAsState(initial = emptyList())
    val notes by app.database.aayaDao().getAllNotes().collectAsState(initial = emptyList())
    val shoppingList by app.database.aayaDao().getNotesByCategory("Shopping").collectAsState(initial = emptyList())
    val expenses by app.database.aayaDao().getAllExpenses().collectAsState(initial = emptyList())
    val scheduledTasks by app.database.aayaDao().getPendingScheduledTasks().collectAsState(initial = emptyList())
    val auditLogs by app.database.aayaDao().getRecentAuditLogs().collectAsState(initial = emptyList())

    if (showVoiceCustomizer) {
        VoiceCustomizerDialog(
            ttsManager = ttsManager,
            onDismiss = { showVoiceCustomizer = false }
        )
    }

    selectedFestivalForDialog?.let { festival ->
        FestivalGreetingDialog(
            festival = festival,
            onDismiss = { selectedFestivalForDialog = null }
        )
    }

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
                    label = { Text("Assistant", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentNavIndex == 1,
                    onClick = { currentNavIndex = 1 },
                    icon = { Icon(Icons.Default.EventNote, contentDescription = "Hub") },
                    label = { Text("Hub", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentNavIndex == 2,
                    onClick = { currentNavIndex = 2 },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = "Memory") },
                    label = { Text("Memory", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentNavIndex == 3,
                    onClick = { currentNavIndex = 3 },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Permissions") },
                    label = { Text("Access", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentNavIndex == 4,
                    onClick = { currentNavIndex = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 10.sp) }
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
                    totalRequests = app.preferenceManager.totalApiRequests,
                    nextTask = scheduledTasks.firstOrNull(),
                    onOrbClick = onToggleVoiceSession,
                    onOpenFeature = { query -> onSuggestionClicked(query) },
                    onNavigateToHub = { currentNavIndex = 1 },
                    onOpenVoiceStudio = { showVoiceCustomizer = true },
                    onOpenFestivalCard = { fest -> selectedFestivalForDialog = fest },
                    onSpeakQuote = { quoteText -> ttsManager.speak(quoteText) }
                )
                1 -> NotesAndScheduleScreen(
                    notes = notes,
                    shoppingList = shoppingList,
                    expenses = expenses,
                    scheduledTasks = scheduledTasks,
                    auditLogs = auditLogs,
                    onToggleShoppingItem = { item ->
                        scope.launch {
                            app.database.aayaDao().updateNote(item.copy(isCompleted = !item.isCompleted))
                        }
                    },
                    onDeleteNote = { item -> scope.launch { app.database.aayaDao().deleteNote(item) } },
                    onDeleteExpense = { exp -> scope.launch { app.database.aayaDao().deleteExpense(exp) } },
                    onDeleteScheduledTask = { task -> scope.launch { app.database.aayaDao().deleteScheduledTask(task) } },
                    onAddNote = { title, content, cat ->
                        scope.launch {
                            app.database.aayaDao().insertNote(
                                NoteItem(title = title, content = content, category = cat)
                            )
                        }
                    },
                    onAddExpense = { amount, cat, desc ->
                        scope.launch {
                            app.database.aayaDao().insertExpense(
                                ExpenseItem(amount = amount, category = cat, description = desc)
                            )
                        }
                    }
                )
                2 -> MemoryDashboardScreen(
                    memories = memories,
                    routines = routines,
                    vipContacts = vips,
                    onDeleteMemory = { item -> scope.launch { app.database.aayaDao().deleteMemory(item) } },
                    onToggleRoutine = { routine -> scope.launch { app.database.aayaDao().updateRoutine(routine) } },
                    onDeleteVip = { vip -> scope.launch { app.database.aayaDao().deleteVipContact(vip) } },
                    onAddMemory = { k, v, cat -> scope.launch { app.database.aayaDao().insertMemory(MemoryItem(category = cat, key = k, value = v)) } },
                    onAddVip = { name, phone, rel -> scope.launch { app.database.aayaDao().insertVipContact(VipContact(name = name, phoneNumber = phone, relationship = rel)) } }
                )
                3 -> PermissionWizardScreen(
                    onAllGranted = {
                        app.preferenceManager.isInitialSetupDone = true
                        currentNavIndex = 0
                    }
                )
                4 -> ApiSettingsScreen()
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
    totalRequests: Int,
    nextTask: ScheduledTask?,
    onOrbClick: () -> Unit,
    onOpenFeature: (String) -> Unit,
    onNavigateToHub: () -> Unit,
    onOpenVoiceStudio: () -> Unit,
    onOpenFestivalCard: (com.aaya.assistant.data.model.FestivalModel) -> Unit,
    onSpeakQuote: (String) -> Unit
) {
    val prefs = AayaApplication.instance.preferenceManager
    val userName = prefs.userName.ifBlank { "Friend" }
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour in 5..11 -> "Good morning, $userName! ☀️"
        hour in 12..16 -> "Good afternoon, $userName! 🌤️"
        hour in 17..21 -> "Good evening, $userName! 🌅"
        else -> "Good night, $userName! 🌙"
    }

    val dailyQuote = remember { QuoteLibrary.getQuoteOfTheDay() }
    val upcomingFestival = remember { FestivalManager.getUpcomingFestival() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header & Personal Greeting
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "AAYA Companion & Lifestyle AI",
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                }
                // Voice Studio Quick Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(RadiantPurple.copy(alpha = 0.25f))
                        .border(1.dp, RadiantPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { onOpenVoiceStudio() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = prefs.voicePreset.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Upcoming Indian Festival Banner
        if (upcomingFestival != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(GoldAccent, NeonCyan, RadiantPurple)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onOpenFestivalCard(upcomingFestival) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Celebration, contentDescription = null, tint = GoldAccent)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FESTIVAL CELEBRATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                letterSpacing = 1.sp
                            )
                            Text(upcomingFestival.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(upcomingFestival.wishesHindi.take(45) + "…", fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Card ➔",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }
            }
        }

        // Bilingual Daily Inspiration Quote Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "आज का सुविचार • Daily Inspiration",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                        IconButton(
                            onClick = { onSpeakQuote("${dailyQuote.hindiText} — ${dailyQuote.author}") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Listen", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "“${dailyQuote.hindiText}”",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "“${dailyQuote.englishText}”",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "— ${dailyQuote.author}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // Privacy & Offline Intelligence Badge
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("100% On-Device Memory", fontSize = 11.sp, color = TextPrimary)
                    }
                    Text("AI Calls: $totalRequests", fontSize = 11.sp, color = NeonCyan)
                }
            }
        }

        // Next Up Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToHub() }
                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (nextTask != null) Icons.Default.Event else Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (nextTask != null) "NEXT UP" else "ALL CLEAR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                        if (nextTask != null) {
                            Text(nextTask.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(nextTask.triggerTimeEpochMs))
                            Text("Scheduled for $timeStr", fontSize = 11.sp, color = TextSecondary)
                        } else {
                            Text("No pending tasks or reminders", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }
        }

        // Animated Voice Orb
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                GlowingVoiceOrb(
                    isListening = isListening,
                    audioLevelRms = audioLevelRms,
                    size = 170.dp,
                    onClick = onOrbClick
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (isListening) "Listening to you…" else "Tap orb or shake device to speak",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isListening) NeonCyan else TextPrimary
                )
            }
        }

        // Quick Command Tiles
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Quick Actions", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard(
                        title = "Hisab-Kitab",
                        subtitle = "Log Expense ₹",
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenFeature("Kitna kharch hua aaj?") }
                    )
                    QuickActionCard(
                        title = "Take Photo",
                        subtitle = "Camera Assistant",
                        icon = Icons.Default.CameraAlt,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenFeature("Take a photo") }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard(
                        title = "Flashlight",
                        subtitle = "Toggle Torch",
                        icon = Icons.Default.FlashlightOn,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenFeature("Turn on flashlight") }
                    )
                    QuickActionCard(
                        title = "Emergency SOS",
                        subtitle = "Strobe Beacon",
                        icon = Icons.Default.Warning,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenFeature("SOS") }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionCard(
                        title = "Shopping",
                        subtitle = "Check List",
                        icon = Icons.Default.ShoppingCart,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenFeature("What is on my shopping list?") }
                    )
                    QuickActionCard(
                        title = "Daily Plan",
                        subtitle = "Classes & Alarms",
                        icon = Icons.Default.CalendarToday,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenFeature("Plan my day") }
                    )
                }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = null
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
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
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text(subtitle, fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}
