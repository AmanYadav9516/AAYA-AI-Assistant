package com.aaya.assistant.engine.router

import android.content.Context
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.data.local.AayaDatabase
import com.aaya.assistant.data.model.MemoryItem
import com.aaya.assistant.data.remote.GeminiClient
import com.aaya.assistant.data.remote.GeminiResult
import com.aaya.assistant.engine.audio.TextToSpeechManager
import com.aaya.assistant.engine.contacts.MultilingualContactMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

        // 1. FAST LOCAL ROUTE: Flashlight / Torch
        if (lower.contains("torch on") || lower.contains("flashlight on") || lower.contains("torch jalao")) {
            val ok = deviceController.toggleFlashlight(true)
            val msg = if (ok) "Flashlight turned on." else "Flashlight couldn't be turned on."
            speak(msg)
            return@withContext ExecutionResult(msg, "Flashlight ON", handledLocally = true)
        }
        if (lower.contains("torch off") || lower.contains("flashlight off") || lower.contains("torch band")) {
            val ok = deviceController.toggleFlashlight(false)
            val msg = if (ok) "Flashlight turned off." else "Flashlight couldn't be turned off."
            speak(msg)
            return@withContext ExecutionResult(msg, "Flashlight OFF", handledLocally = true)
        }

        // 2. FAST LOCAL ROUTE: Volume
        if (lower.contains("volume up") || lower.contains("awaz badhao")) {
            deviceController.adjustVolume(increase = true)
            speak("Volume increased.")
            return@withContext ExecutionResult("Volume increased.", "Volume UP", handledLocally = true)
        }
        if (lower.contains("volume down") || lower.contains("awaz kam karo")) {
            deviceController.adjustVolume(increase = false)
            speak("Volume decreased.")
            return@withContext ExecutionResult("Volume decreased.", "Volume DOWN", handledLocally = true)
        }

        // 3. FAST LOCAL ROUTE: Calling & Multilingual Contact Match
        if (lower.startsWith("call") || lower.contains("ko call") || lower.contains("ko phone") || lower.contains("phone lagao")) {
            val target = contactMatcher.extractTargetName(query)
            val match = contactMatcher.resolveAndFindContact(target)
            if (match != null) {
                if (match.confidence >= 0.95f) {
                    val msg = "Calling ${match.contactName}."
                    speak(msg)
                    deviceController.placeCall(match.phoneNumber)
                    return@withContext ExecutionResult(msg, "Call -> ${match.contactName}", handledLocally = true)
                } else if (match.needsConfirmation) {
                    val msg = "I found ${match.contactName}. Should I call?"
                    speak(msg)
                    return@withContext ExecutionResult(msg, "Confirm call -> ${match.contactName}", handledLocally = true)
                }
            }
        }

        // 4. FAST LOCAL ROUTE: Quick App Launch ("Open WhatsApp", "Open YouTube")
        if (lower.startsWith("open ") || lower.startsWith("kholo ")) {
            val appName = lower.removePrefix("open ").removePrefix("kholo ").trim()
            val opened = deviceController.openAppByName(appName)
            if (opened) {
                val msg = "Opening $appName."
                speak(msg)
                return@withContext ExecutionResult(msg, "Open $appName", handledLocally = true)
            }
        }

        // 5. FAST LOCAL ROUTE: Modes (Sleep / DND / Normal)
        if (lower.contains("sleep mode on") || lower.contains("so raha hu") || lower.contains("sleeping mode")) {
            deviceController.setDoNotDisturb(true)
            val msg = "Sleep mode activated. Alarms and VIP calls remain active."
            speak(msg)
            return@withContext ExecutionResult(msg, "Mode: SLEEP", handledLocally = true)
        }
        if (lower.contains("normal mode") || lower.contains("wake up mode") || lower.contains("silent hatao")) {
            deviceController.setDoNotDisturb(false)
            val msg = "Normal mode restored."
            speak(msg)
            return@withContext ExecutionResult(msg, "Mode: NORMAL", handledLocally = true)
        }

        // 6. COMPLEX / AI ROUTE: Forward to Gemini AI Brain with Tool Calling
        val userMemories = try {
            db.aayaDao().getMemoryByCategory("routine")
        } catch (e: Exception) {
            emptyList()
        }
        val contextSummary = userMemories.joinToString("; ") { "${it.key}: ${it.value}" }

        when (val result = geminiClient.executeVoicePrompt(query, contextSummary)) {
            is GeminiResult.Success -> {
                // Execute any invoked tools
                for (tool in result.functionCalls) {
                    when (tool.name) {
                        "make_call" -> {
                            val contact = tool.args["contact_name"]?.toString() ?: ""
                            val match = contactMatcher.resolveAndFindContact(contact)
                            if (match != null) {
                                deviceController.placeCall(match.phoneNumber)
                            }
                        }
                        "open_app" -> {
                            val app = tool.args["app_name"]?.toString() ?: ""
                            deviceController.openAppByName(app)
                        }
                        "toggle_flashlight" -> {
                            val state = tool.args["state"]?.toString() ?: "on"
                            deviceController.toggleFlashlight(state.equals("on", ignoreCase = true))
                        }
                        "set_smart_alarm" -> {
                            val time = tool.args["time_string"]?.toString() ?: "07:00"
                            parseAndSetAlarm(time)
                        }
                        "set_lifestyle_mode" -> {
                            val mode = tool.args["mode"]?.toString() ?: "SLEEP"
                            deviceController.setDoNotDisturb(mode != "NORMAL")
                        }
                        "remember_fact" -> {
                            val k = tool.args["key"]?.toString() ?: "preference"
                            val v = tool.args["value"]?.toString() ?: ""
                            val cat = tool.args["category"]?.toString() ?: "fact"
                            db.aayaDao().insertMemory(MemoryItem(category = cat, key = k, value = v))
                        }
                    }
                }

                val reply = result.responseText.ifEmpty { "Command executed." }
                speak(reply)
                ExecutionResult(reply, "Gemini AI Brain (tools: ${result.functionCalls.size})", handledLocally = false)
            }
            is GeminiResult.Error -> {
                val fallbackMsg = "I couldn't reach the network. Please check your API key in Settings."
                speak(fallbackMsg)
                ExecutionResult(fallbackMsg, "API Error: ${result.errorMessage}", handledLocally = false)
            }
        }
    }

    private fun speak(text: String) {
        ttsManager.speak(text)
    }

    private fun parseAndSetAlarm(timeString: String) {
        try {
            val parts = timeString.replace("[^0-9:]".toRegex(), "").split(":")
            if (parts.isNotEmpty()) {
                val hour = parts[0].toIntOrNull() ?: 7
                val minute = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
                deviceController.setAlarm(hour, minute, "AAYA Alarm")
            }
        } catch (e: Exception) {
            // Ignore parse failure
        }
    }
}
