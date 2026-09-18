package com.aaya.assistant.engine.router

import android.content.Context
import com.aaya.assistant.data.local.AayaDatabase
import com.aaya.assistant.data.model.AuditLogItem
import com.aaya.assistant.data.model.MemoryItem
import com.aaya.assistant.data.model.NoteItem
import com.aaya.assistant.data.model.ScheduledTask
import com.aaya.assistant.data.remote.FunctionCallRequest
import com.aaya.assistant.data.remote.GeminiClient
import com.aaya.assistant.data.remote.GeminiResult
import com.aaya.assistant.engine.audio.TextToSpeechManager
import com.aaya.assistant.engine.contacts.MultilingualContactMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class ExecutionResult(
    val speechResponse: String,
    val actionSummary: String? = null,
    val handledLocally: Boolean = false
)

class CommandRouter(
    private val context: Context,
    private val ttsManager: TextToSpeechManager,
    private val geminiClient: GeminiClient,
    private val contactMatcher: MultilingualContactMatcher
) {
    private val deviceController = DeviceController(context)
    private val db = AayaDatabase.getInstance(context)

    suspend fun routeAndExecute(rawSpokenText: String): ExecutionResult = withContext(Dispatchers.IO) {
        val query = rawSpokenText.trim()
        val lower = query.lowercase()

        // 1. FAST LOCAL ROUTE: Flashlight / Torch (100% Offline)
        val isTorchCommand = lower.contains("torch") || lower.contains("flashlight")
        if (isTorchCommand) {
            val isOff = lower.contains("off") || lower.contains("band") || lower.contains("stop") || lower.contains("close")
            val isOn = lower.contains("on") || lower.contains("turn on") || lower.contains("jalao") || lower.contains("chalu") || lower.contains("start") || !isOff
            val ok = deviceController.toggleFlashlight(isOn)
            val msg = if (isOn) {
                if (ok) "Flashlight turned on." else "Flashlight couldn't be turned on."
            } else {
                if (ok) "Flashlight turned off." else "Flashlight couldn't be turned off."
            }
            logAudit("MODE", if (isOn) "Turned flashlight ON" else "Turned flashlight OFF")
            speak(msg)
            return@withContext ExecutionResult(msg, if (isOn) "Flashlight ON" else "Flashlight OFF", handledLocally = true)
        }

        // 2. FAST LOCAL ROUTE: Camera Assistant
        if (lower.contains("take a photo") || lower.contains("open camera") || lower.contains("camera kholo") ||
            lower.contains("take a selfie") || lower.contains("selfie lo") || lower.contains("record a video") || lower.contains("video banao")) {
            
            if (!deviceController.hasCameraPermission()) {
                val msg = "I need Camera permission to open the camera. You can enable it from the Permissions tab."
                speak(msg)
                return@withContext ExecutionResult(msg, "Missing Camera Permission", handledLocally = true)
            }

            if (lower.contains("10 second") || lower.contains("10 sec") || lower.contains("timer")) {
                speak("Taking photo in 10 seconds.")
                delay(10000)
                deviceController.openCamera()
                logAudit("CAMERA", "Took photo with 10s timer")
                return@withContext ExecutionResult("Photo captured.", "Camera 10s Timer", handledLocally = true)
            } else if (lower.contains("selfie")) {
                deviceController.takeSelfie()
                val msg = "Opening front camera."
                speak(msg)
                logAudit("CAMERA", "Opened Selfie Camera")
                return@withContext ExecutionResult(msg, "Selfie Camera", handledLocally = true)
            } else if (lower.contains("video")) {
                deviceController.recordVideo()
                val msg = "Opening video recorder."
                speak(msg)
                logAudit("CAMERA", "Opened Video Camera")
                return@withContext ExecutionResult(msg, "Video Camera", handledLocally = true)
            } else {
                deviceController.openCamera()
                val msg = "Opening camera."
                speak(msg)
                logAudit("CAMERA", "Opened Camera")
                return@withContext ExecutionResult(msg, "Open Camera", handledLocally = true)
            }
        }

        // 3. FAST LOCAL ROUTE: "What Did You Do?" (Audit & Trust Log)
        if (lower.contains("what did you do") || lower.contains("aaj kya kiya") || lower.contains("activity log") || lower.contains("show audit")) {
            val startOfDay = getStartOfDayMillis()
            val logs = db.aayaDao().getAuditLogsSince(startOfDay)
            val alarmCount = logs.count { it.actionType == "ALARM" }
            val remCount = logs.count { it.actionType == "REMINDER" }
            val noteCount = logs.count { it.actionType == "NOTE" }
            val modeCount = logs.count { it.actionType == "MODE" }
            val aiCount = logs.count { it.actionType == "AI_REQUEST" }

            val msg = buildString {
                append("Today, ")
                val parts = mutableListOf<String>()
                if (alarmCount > 0) parts.add("I set $alarmCount alarm${if (alarmCount > 1) "s" else ""}")
                if (remCount > 0) parts.add("created $remCount reminder${if (remCount > 1) "s" else ""}")
                if (modeCount > 0) parts.add("activated focus/sleep modes $modeCount time${if (modeCount > 1) "s" else ""}")
                if (noteCount > 0) parts.add("saved $noteCount note${if (noteCount > 1) "s" else ""}")
                if (aiCount > 0) parts.add("processed $aiCount AI requests")
                if (parts.isEmpty()) {
                    append("I haven't performed any actions yet today.")
                } else {
                    append(parts.joinToString(", "))
                    append(".")
                }
            }
            speak(msg)
            return@withContext ExecutionResult(msg, "Audit: ${logs.size} actions today", handledLocally = true)
        }

        // 4. FAST LOCAL ROUTE: Shopping List Quick Access
        if (lower.contains("shopping list") || lower.contains("kharidna hai") || lower.contains("bazaar")) {
            if (lower.startsWith("add ") || lower.contains(" add ") || lower.contains("daal do") || lower.contains("jodo")) {
                val itemsRaw = query.substringAfter("add", "").substringBefore("to my shopping", "").trim()
                val items = if (itemsRaw.isNotBlank()) itemsRaw else query.replace("add to shopping list", "", ignoreCase = true).trim()
                val splitItems = items.split(",", " and ", " aur ").map { it.trim() }.filter { it.isNotBlank() }
                for (item in splitItems) {
                    db.aayaDao().insertNote(
                        NoteItem(
                            title = item,
                            content = item,
                            category = "Shopping",
                            isCompleted = false
                        )
                    )
                }
                val msg = "Added ${splitItems.joinToString(", ")} to your shopping list."
                logAudit("NOTE", "Added items to Shopping list: ${splitItems.joinToString(", ")}")
                speak(msg)
                return@withContext ExecutionResult(msg, "Shopping: +${splitItems.size} items", handledLocally = true)
            } else if (lower.contains("what is on") || lower.contains("kya hai") || lower.contains("show") || lower.contains("read")) {
                val shoppingItems = db.aayaDao().getNotesByCategorySync("Shopping").filter { !it.isCompleted }
                val msg = if (shoppingItems.isEmpty()) {
                    "Your shopping list is currently empty."
                } else {
                    "Your shopping list has: ${shoppingItems.joinToString(", ") { it.content }}."
                }
                speak(msg)
                return@withContext ExecutionResult(msg, "Shopping list (${shoppingItems.size} items)", handledLocally = true)
            } else if (lower.contains("remove ") || lower.contains("hatao ") || lower.contains("mark ")) {
                val target = lower.removePrefix("remove ").removePrefix("hatao ").removePrefix("mark ").removeSuffix(" as purchased").removeSuffix(" from shopping list").trim()
                val removed = db.aayaDao().removeShoppingItem(target)
                val msg = if (removed > 0) "Removed $target from shopping list." else "Could not find $target on your shopping list."
                speak(msg)
                return@withContext ExecutionResult(msg, "Shopping: remove $target", handledLocally = true)
            }
        }

        // 5. FAST LOCAL ROUTE: Settings & Connectivity (100% Offline)
        if (lower.contains("setting") || lower.contains("settings")) {
            deviceController.openSettings()
            val msg = "Opening Settings."
            speak(msg)
            return@withContext ExecutionResult(msg, "Open Settings", handledLocally = true)
        }

        if (lower.contains("wi-fi") || lower.contains("wifi") || lower.contains("wi fi")) {
            deviceController.openWifiSettings()
            val msg = "Opening Wi-Fi Settings."
            speak(msg)
            return@withContext ExecutionResult(msg, "Wi-Fi Settings", handledLocally = true)
        }

        if (lower.contains("bluetooth") || lower.contains("blutooth") || lower.contains("blooth")) {
            deviceController.openBluetoothSettings()
            val msg = "Opening Bluetooth Settings."
            speak(msg)
            return@withContext ExecutionResult(msg, "Bluetooth Settings", handledLocally = true)
        }

        // 6. FAST LOCAL ROUTE: Volume
        if (lower.contains("volume up") || lower.contains("awaz badhao") || lower.contains("sound up")) {
            deviceController.adjustVolume(increase = true)
            speak("Volume increased.")
            return@withContext ExecutionResult("Volume increased.", "Volume UP", handledLocally = true)
        }
        if (lower.contains("volume down") || lower.contains("awaz kam karo") || lower.contains("sound down")) {
            deviceController.adjustVolume(increase = false)
            speak("Volume decreased.")
            return@withContext ExecutionResult("Volume decreased.", "Volume DOWN", handledLocally = true)
        }

        // 7. FAST LOCAL ROUTE: Permission-Aware Calling & Multilingual Contact Match
        if (lower.startsWith("call") || lower.contains("ko call") || lower.contains("ko phone") || lower.contains("phone lagao")) {
            val target = contactMatcher.extractTargetName(query)

            // Permission-Aware Check
            if (!deviceController.hasContactPermission()) {
                val msg = "I need Contacts access to identify $target. You can enable it from the Permissions tab."
                speak(msg)
                return@withContext ExecutionResult(msg, "Missing Contacts Permission", handledLocally = true)
            }

            if (!deviceController.hasPhonePermission()) {
                val msg = "I need Phone permission to place direct calls. You can enable it from the Permissions tab."
                speak(msg)
                return@withContext ExecutionResult(msg, "Missing Phone Permission", handledLocally = true)
            }

            val match = contactMatcher.resolveAndFindContact(target)
            if (match != null) {
                if (match.confidence >= 0.95f) {
                    val msg = "Calling ${match.contactName}."
                    speak(msg)
                    deviceController.placeCall(match.phoneNumber)
                    logAudit("CALL", "Placed call to ${match.contactName}")
                    return@withContext ExecutionResult(msg, "Call -> ${match.contactName}", handledLocally = true)
                } else if (match.needsConfirmation) {
                    val msg = "I found ${match.contactName}. Should I call?"
                    speak(msg)
                    return@withContext ExecutionResult(msg, "Confirm call -> ${match.contactName}", handledLocally = true)
                }
            }
        }

        // 8. FAST LOCAL ROUTE: Quick App Launch ("Open WhatsApp", "Open YouTube")
        if (lower.startsWith("open ") || lower.startsWith("kholo ") || lower.startsWith("launch ")) {
            val appName = lower.removePrefix("open ").removePrefix("kholo ").removePrefix("launch ").trim()
            val opened = deviceController.openAppByName(appName)
            if (opened) {
                val msg = "Opening $appName."
                speak(msg)
                logAudit("MODE", "Opened app: $appName")
                return@withContext ExecutionResult(msg, "Open $appName", handledLocally = true)
            }
        }

        // 9. FAST LOCAL ROUTE: Modes (Sleep / Focus / DND / Normal)
        if (lower.contains("sleep mode on") || lower.contains("so raha hu") || lower.contains("sleeping mode")) {
            if (!deviceController.hasDndPermission()) {
                val msg = "I need Do Not Disturb permission to activate Sleep Mode. You can enable it in Permissions."
                speak(msg)
                return@withContext ExecutionResult(msg, "Missing DND Permission", handledLocally = true)
            }
            deviceController.setDoNotDisturb(true)
            val msg = "Sleep mode activated. Alarms and VIP calls remain active."
            logAudit("MODE", "Activated Sleep Mode")
            speak(msg)
            return@withContext ExecutionResult(msg, "Mode: SLEEP", handledLocally = true)
        }

        // 10. FAST LOCAL ROUTE: Study Focus Timer
        if (lower.contains("study session") || lower.contains("start studying") || lower.contains("focus mode")) {
            val durationMinutes = extractMinutes(lower) ?: 45
            deviceController.setTimer(durationMinutes * 60, "AAYA Study Session")
            if (deviceController.hasDndPermission()) {
                deviceController.setDoNotDisturb(true)
            }
            val msg = "Starting a $durationMinutes-minute focused study session. Notifications are silenced."
            logAudit("MODE", "Started $durationMinutes min Study Session")
            speak(msg)
            return@withContext ExecutionResult(msg, "Study Session ($durationMinutes mins)", handledLocally = true)
        }

        // 11. FAST LOCAL ROUTE: Daily Planner
        if (lower.contains("plan my day") || lower.contains("plan my tomorrow") || lower.contains("what is my plan") || lower.contains("aaj ka plan")) {
            val isTomorrow = lower.contains("tomorrow") || lower.contains("kal")
            val targetDay = if (isTomorrow) getTomorrowDayName() else getTodayDayName()
            val timetable = db.aayaDao().getTimetableForDay(targetDay)
            val pendingTasks = db.aayaDao().getPendingScheduledTasksSync()

            val msg = buildString {
                append(if (isTomorrow) "Here is your plan for tomorrow: " else "Here is your plan for today: ")
                val parts = mutableListOf<String>()
                if (timetable.isNotEmpty()) {
                    parts.add("Classes: " + timetable.joinToString(", ") { "${it.subject} at ${it.startTime}" })
                }
                if (pendingTasks.isNotEmpty()) {
                    parts.add("Scheduled Tasks: " + pendingTasks.take(3).joinToString(", ") { it.title })
                }
                if (parts.isEmpty()) {
                    append("You have no scheduled classes or reminders recorded.")
                } else {
                    append(parts.joinToString(". "))
                }
            }
            speak(msg)
            return@withContext ExecutionResult(msg, "Daily Plan ($targetDay)", handledLocally = true)
        }

        // 12. COMPLEX / AI ROUTE: Forward to Gemini 3.6 Flash AI Brain with Multi-Action Tool Calling
        val userMemories = try {
            db.aayaDao().getMemoryByCategory("routine")
        } catch (e: Exception) {
            emptyList()
        }
        val contextSummary = userMemories.joinToString("; ") { "${it.key}: ${it.value}" }

        when (val result = geminiClient.executeVoicePrompt(query, contextSummary)) {
            is GeminiResult.Success -> {
                logAudit("AI_REQUEST", "Query: '$query' (${result.functionCalls.size} tools)")

                val executedSummaries = mutableListOf<String>()

                // MULTI-ACTION EXECUTION LOOP: Execute ALL tools returned by Gemini
                for (tool in result.functionCalls) {
                    executeToolCall(tool, executedSummaries)
                }

                val reply = if (result.responseText.isNotEmpty()) {
                    result.responseText
                } else if (executedSummaries.isNotEmpty()) {
                    executedSummaries.joinToString(". ") + "."
                } else {
                    "Done."
                }

                speak(reply)
                ExecutionResult(
                    speechResponse = reply,
                    actionSummary = if (executedSummaries.isNotEmpty()) executedSummaries.joinToString(" | ") else "Gemini AI Brain",
                    handledLocally = false
                )
            }
            is GeminiResult.Error -> {
                val fallbackMsg = "I couldn't reach the AI server. Please check your network or API key in Settings."
                speak(fallbackMsg)
                ExecutionResult(fallbackMsg, "API Error: ${result.errorMessage}", handledLocally = false)
            }
        }
    }

    private suspend fun executeToolCall(tool: FunctionCallRequest, executedSummaries: MutableList<String>) {
        when (tool.name) {
            "make_call" -> {
                val contact = tool.args["contact_name"]?.toString() ?: ""
                if (!deviceController.hasContactPermission() || !deviceController.hasPhonePermission()) {
                    executedSummaries.add("Contacts permission needed to call $contact")
                } else {
                    val match = contactMatcher.resolveAndFindContact(contact)
                    if (match != null) {
                        deviceController.placeCall(match.phoneNumber)
                        logAudit("CALL", "Called ${match.contactName}")
                        executedSummaries.add("Calling ${match.contactName}")
                    }
                }
            }
            "open_app" -> {
                val app = tool.args["app_name"]?.toString() ?: ""
                deviceController.openAppByName(app)
                logAudit("MODE", "Opened $app")
                executedSummaries.add("Opened $app")
            }
            "toggle_flashlight" -> {
                val state = tool.args["state"]?.toString() ?: "on"
                val on = state.equals("on", ignoreCase = true)
                deviceController.toggleFlashlight(on)
                logAudit("MODE", "Toggled flashlight $state")
                executedSummaries.add("Flashlight $state")
            }
            "set_smart_alarm" -> {
                val time = tool.args["time_string"]?.toString() ?: "07:00"
                val label = tool.args["label"]?.toString() ?: "AAYA Alarm"
                parseAndSetAlarm(time, label)
                logAudit("ALARM", "Set alarm for $time ($label)")
                executedSummaries.add("Alarm set for $time")
            }
            "set_lifestyle_mode" -> {
                val mode = tool.args["mode"]?.toString() ?: "SLEEP"
                deviceController.setDoNotDisturb(mode != "NORMAL")
                logAudit("MODE", "Set mode: $mode")
                executedSummaries.add("Mode: $mode")
            }
            "remember_fact" -> {
                val k = tool.args["key"]?.toString() ?: "fact"
                val v = tool.args["value"]?.toString() ?: ""
                val cat = tool.args["category"]?.toString() ?: "fact"
                db.aayaDao().insertMemory(MemoryItem(category = cat, key = k, value = v))
                logAudit("NOTE", "Saved memory: $k = $v")
                executedSummaries.add("Saved to memory")
            }
            "save_note" -> {
                val title = tool.args["title"]?.toString() ?: "Note"
                val content = tool.args["content"]?.toString() ?: ""
                val cat = tool.args["category"]?.toString() ?: "Notes"
                db.aayaDao().insertNote(NoteItem(title = title, content = content, category = cat))
                logAudit("NOTE", "Saved note ($cat): $title")
                executedSummaries.add("Saved note under $cat")
            }
            "manage_shopping_list" -> {
                val action = tool.args["action"]?.toString() ?: "add"
                val items = tool.args["items"]?.toString() ?: ""
                when (action) {
                    "add" -> {
                        val list = items.split(",", " and ").map { it.trim() }.filter { it.isNotBlank() }
                        for (item in list) {
                            db.aayaDao().insertNote(NoteItem(title = item, content = item, category = "Shopping"))
                        }
                        logAudit("NOTE", "Added to shopping list: $items")
                        executedSummaries.add("Added to shopping list")
                    }
                    "remove" -> {
                        db.aayaDao().removeShoppingItem(items)
                        executedSummaries.add("Removed $items from shopping")
                    }
                }
            }
            "schedule_action" -> {
                val title = tool.args["title"]?.toString() ?: "Reminder"
                val timeStr = tool.args["time_string"]?.toString() ?: ""
                val type = tool.args["task_type"]?.toString() ?: "CUSTOM_REMINDER"
                val target = tool.args["target_contact"]?.toString() ?: ""
                val triggerEpochMs = parseTimeExpressionToEpoch(timeStr)
                val taskId = db.aayaDao().insertScheduledTask(
                    ScheduledTask(
                        title = title,
                        taskType = type,
                        targetData = target,
                        triggerTimeEpochMs = triggerEpochMs
                    )
                )
                deviceController.scheduleTaskNotification(triggerEpochMs, taskId, title, type, target)
                logAudit("REMINDER", "Scheduled: $title for $timeStr")
                executedSummaries.add("Scheduled $title")
            }
            "start_study_timer" -> {
                val duration = (tool.args["duration_minutes"] as? Number)?.toInt() ?: 45
                deviceController.setTimer(duration * 60, "AAYA Study Session")
                if (deviceController.hasDndPermission()) {
                    deviceController.setDoNotDisturb(true)
                }
                logAudit("MODE", "Started $duration min Study Session")
                executedSummaries.add("Study session ($duration min) started")
            }
            "camera_action" -> {
                val mode = tool.args["mode"]?.toString() ?: "photo"
                when (mode) {
                    "selfie" -> deviceController.takeSelfie()
                    "video" -> deviceController.recordVideo()
                    else -> deviceController.openCamera()
                }
                logAudit("CAMERA", "Triggered camera: $mode")
                executedSummaries.add("Camera: $mode")
            }
            "web_search" -> {
                val query = tool.args["query"]?.toString() ?: ""
                deviceController.searchWeb(query)
                logAudit("AI_REQUEST", "Web Search: $query")
                executedSummaries.add("Searching web for $query")
            }
        }
    }

    private suspend fun logAudit(type: String, summary: String) {
        try {
            db.aayaDao().insertAuditLog(AuditLogItem(actionType = type, summary = summary))
        } catch (e: Exception) {
            // Ignore DB log failure
        }
    }

    private fun speak(text: String) {
        ttsManager.speak(text)
    }

    private fun parseAndSetAlarm(timeString: String, label: String = "AAYA Alarm") {
        try {
            val parts = timeString.replace("[^0-9:]".toRegex(), "").split(":")
            if (parts.isNotEmpty()) {
                val hour = parts[0].toIntOrNull() ?: 7
                val minute = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
                deviceController.setAlarm(hour, minute, label)
            }
        } catch (e: Exception) {
            // Ignore parse failure
        }
    }

    private fun parseTimeExpressionToEpoch(timeStr: String): Long {
        val now = Calendar.getInstance()
        val lower = timeStr.lowercase()

        // Handle "in X minutes"
        if (lower.contains("minute")) {
            val mins = extractMinutes(lower) ?: 30
            return System.currentTimeMillis() + (mins * 60 * 1000L)
        }

        // Handle "at 7 PM" or "7:00"
        val isPm = lower.contains("pm")
        val isTomorrow = lower.contains("tomorrow")
        val digits = timeStr.replace("[^0-9:]".toRegex(), "").split(":")

        if (digits.isNotEmpty()) {
            var hour = digits[0].toIntOrNull() ?: 9
            val minute = if (digits.size > 1) digits[1].toIntOrNull() ?: 0 else 0
            if (isPm && hour < 12) hour += 12
            now.set(Calendar.HOUR_OF_DAY, hour)
            now.set(Calendar.MINUTE, minute)
            now.set(Calendar.SECOND, 0)
            if (isTomorrow || now.timeInMillis <= System.currentTimeMillis()) {
                now.add(Calendar.DAY_OF_YEAR, 1)
            }
            return now.timeInMillis
        }

        // Default to 1 hour from now
        return System.currentTimeMillis() + (60 * 60 * 1000L)
    }

    private fun extractMinutes(text: String): Int? {
        val match = Regex("(\\d+)\\s*(min|minute)").find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun getStartOfDayMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getTodayDayName(): String {
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
    }

    private fun getTomorrowDayName(): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.time)
    }
}
