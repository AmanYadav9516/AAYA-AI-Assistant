package com.aaya.assistant.data.remote

import com.aaya.assistant.data.local.PreferenceManager
import com.aaya.assistant.data.model.ApiDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiResult {
    data class Success(
        val responseText: String,
        val functionCalls: List<FunctionCallRequest>,
        val latencyMs: Long
    ) : GeminiResult()

    data class Error(
        val errorMessage: String,
        val errorCode: Int? = null
    ) : GeminiResult()
}

data class FunctionCallRequest(
    val name: String,
    val args: Map<String, Any?>
)

class GeminiClient(private val preferenceManager: PreferenceManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Multi-turn conversational memory buffer (holds recent turns for context)
    private val conversationHistory = mutableListOf<JSONObject>()

    fun clearHistory() {
        conversationHistory.clear()
    }

    suspend fun testConnection(): ApiDiagnostics = withContext(Dispatchers.IO) {
        val apiKey = preferenceManager.apiKey.ifEmpty { DEFAULT_FALLBACK_KEY }
        if (apiKey.isBlank()) {
            return@withContext ApiDiagnostics(
                isConnected = false,
                lastErrorMessage = "No API Key configured. Please enter your key in Settings."
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val payload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Ping. Respond with 'PONG'."))
                        })
                    })
                })
            }

            val request = buildRequest(apiKey, payload)
            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    preferenceManager.totalApiRequests += 1
                    preferenceManager.successfulApiRequests += 1
                    preferenceManager.lastApiLatencyMs = latency
                    preferenceManager.lastApiError = null

                    ApiDiagnostics(
                        isConnected = true,
                        totalRequestsSent = preferenceManager.totalApiRequests,
                        successfulRequests = preferenceManager.successfulApiRequests,
                        failedRequests = preferenceManager.totalApiRequests - preferenceManager.successfulApiRequests,
                        lastLatencyMs = latency,
                        lastResponseSummary = "Connected successfully (${response.code})"
                    )
                } else {
                    preferenceManager.totalApiRequests += 1
                    val errorMsg = "HTTP ${response.code}: $body"
                    preferenceManager.lastApiError = errorMsg

                    ApiDiagnostics(
                        isConnected = false,
                        totalRequestsSent = preferenceManager.totalApiRequests,
                        successfulRequests = preferenceManager.successfulApiRequests,
                        failedRequests = preferenceManager.totalApiRequests - preferenceManager.successfulApiRequests,
                        lastLatencyMs = latency,
                        lastErrorMessage = errorMsg
                    )
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            preferenceManager.totalApiRequests += 1
            preferenceManager.lastApiError = e.localizedMessage

            ApiDiagnostics(
                isConnected = false,
                totalRequestsSent = preferenceManager.totalApiRequests,
                successfulRequests = preferenceManager.successfulApiRequests,
                failedRequests = preferenceManager.totalApiRequests - preferenceManager.successfulApiRequests,
                lastLatencyMs = latency,
                lastErrorMessage = e.localizedMessage ?: "Unknown network exception"
            )
        }
    }

    suspend fun executeVoicePrompt(
        userQuery: String,
        systemContext: String = ""
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = preferenceManager.apiKey.ifEmpty { DEFAULT_FALLBACK_KEY }
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("API Key missing. Please set it in Settings.")
        }

        val startTime = System.currentTimeMillis()

        try {
            // Build multi-turn contents
            val contentsArray = JSONArray()
            for (prevTurn in conversationHistory) {
                contentsArray.put(prevTurn)
            }
            val currentTurn = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", userQuery))
                })
            }
            contentsArray.put(currentTurn)

            val rootJson = JSONObject().apply {
                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", buildSystemPrompt(systemContext)))
                    })
                })

                put("contents", contentsArray)

                // Tool Declarations
                put("tools", buildToolDeclarations())
            }

            val request = buildRequest(apiKey, rootJson)
            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val responseString = response.body?.string() ?: ""

                preferenceManager.totalApiRequests += 1

                if (!response.isSuccessful) {
                    val errorDetail = "API Error ${response.code}: $responseString"
                    preferenceManager.lastApiError = errorDetail
                    return@withContext GeminiResult.Error(errorDetail, response.code)
                }

                preferenceManager.successfulApiRequests += 1
                preferenceManager.lastApiLatencyMs = latency
                preferenceManager.lastApiError = null

                // Parse Candidates & Function Calls
                val responseJson = JSONObject(responseString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext GeminiResult.Success("I didn't catch that clearly. Could you repeat?", emptyList(), latency)
                }

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts") ?: JSONArray()

                val functionCalls = mutableListOf<FunctionCallRequest>()
                val textBuilder = StringBuilder()

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        textBuilder.append(part.getString("text"))
                    } else if (part.has("functionCall")) {
                        val fnObj = part.getJSONObject("functionCall")
                        val name = fnObj.getString("name")
                        val argsObj = fnObj.optJSONObject("args") ?: JSONObject()
                        val argsMap = mutableMapOf<String, Any?>()
                        val keys = argsObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            argsMap[k] = argsObj.get(k)
                        }
                        functionCalls.add(FunctionCallRequest(name, argsMap))
                    }
                }

                val finalResponseText = textBuilder.toString().trim()

                // Save turn to multi-turn conversation history
                conversationHistory.add(currentTurn)
                if (finalResponseText.isNotEmpty()) {
                    conversationHistory.add(JSONObject().apply {
                        put("role", "model")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", finalResponseText))
                        })
                    })
                }
                while (conversationHistory.size > 8) {
                    conversationHistory.removeAt(0)
                }

                GeminiResult.Success(
                    responseText = finalResponseText,
                    functionCalls = functionCalls,
                    latencyMs = latency
                )
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Network connection failed"
            preferenceManager.totalApiRequests += 1
            preferenceManager.lastApiError = errorMsg
            GeminiResult.Error(errorMsg)
        }
    }

    private fun buildRequest(apiKey: String, payload: JSONObject): Request {
        val requestBody = payload.toString().toRequestBody(jsonMediaType)
        val cleanKey = apiKey.trim()

        // Google Gemini API standard: accepts all keys via x-goog-api-key header & URL query param
        // Upgraded to gemini-3.6-flash for fast, active, error-free execution
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$cleanKey"
        return Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", cleanKey)
            .post(requestBody)
            .build()
    }

    private fun buildSystemPrompt(userContext: String): String {
        return """
            You are AAYA, an elite voice and lifestyle AI assistant for Android.
            Your answers are concise, friendly, and optimized for voice speech (TTS).
            Keep spoken voice answers under 1-2 sentences. Never reply with verbose paragraphs.
            Understand multilingual phrases in English, Hindi, and Hinglish (e.g., 'Mummy ko call karo', 'Papa ko phone lagao', 'Silent mode on karo', 'Ye note save karo').
            When a user requests one or multiple device actions (e.g. 'Set alarm for 7, turn on torch, and activate sleep mode'), invoke all relevant function calls simultaneously.
            
            Current User Lifestyle & Memory Context:
            $userContext
        """.trimIndent()
    }

    private fun buildToolDeclarations(): JSONArray {
        return JSONArray().apply {
            put(JSONObject().apply {
                put("function_declarations", JSONArray().apply {
                    // Make Call
                    put(JSONObject().apply {
                        put("name", "make_call")
                        put("description", "Place a phone call to a contact or relationship")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("contact_name", JSONObject().put("type", "string").put("description", "Name or relationship (e.g. Mummy, Dad, Alex)"))
                                put("relationship", JSONObject().put("type", "string").put("description", "Normalized relation: Mother, Father, Brother, Sister, Friend"))
                            })
                            put("required", JSONArray().put("contact_name"))
                        })
                    })

                    // Open App
                    put(JSONObject().apply {
                        put("name", "open_app")
                        put("description", "Open any installed application on the device")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("app_name", JSONObject().put("type", "string").put("description", "Name of the app (e.g. WhatsApp, YouTube, Camera)"))
                            })
                            put("required", JSONArray().put("app_name"))
                        })
                    })

                    // Flashlight
                    put(JSONObject().apply {
                        put("name", "toggle_flashlight")
                        put("description", "Turn the flashlight torch on or off")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("state", JSONObject().put("type", "string").put("description", "'on' or 'off'"))
                            })
                            put("required", JSONArray().put("state"))
                        })
                    })

                    // Smart Alarm
                    put(JSONObject().apply {
                        put("name", "set_smart_alarm")
                        put("description", "Set an alarm for a specific time or calculated sleep duration")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("time_string", JSONObject().put("type", "string").put("description", "Time string e.g. '07:30' or '7:00 AM'"))
                                put("label", JSONObject().put("type", "string").put("description", "Alarm reason or label"))
                            })
                            put("required", JSONArray().put("time_string"))
                        })
                    })

                    // Lifestyle Mode
                    put(JSONObject().apply {
                        put("name", "set_lifestyle_mode")
                        put("description", "Activate Sleep Mode, Class Mode, Study Mode, or Driving Mode")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("mode", JSONObject().put("type", "string").put("description", "'SLEEP', 'CLASS', 'STUDY', 'DRIVING', 'NORMAL'"))
                                put("duration_hours", JSONObject().put("type", "integer").put("description", "Duration in hours if temporary"))
                            })
                            put("required", JSONArray().put("mode"))
                        })
                    })

                    // Save Memory
                    put(JSONObject().apply {
                        put("name", "remember_fact")
                        put("description", "Store a user habit, schedule, or fact into memory")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("key", JSONObject().put("type", "string").put("description", "Subject e.g. 'college_schedule', 'favorite_coffee'"))
                                put("value", JSONObject().put("type", "string").put("description", "Detail e.g. '9 AM to 4 PM'"))
                                put("category", JSONObject().put("type", "string").put("description", "'routine', 'preference', 'fact'"))
                            })
                            put("required", JSONArray().put("key").put("value"))
                        })
                    })

                    // Smart Notes
                    put(JSONObject().apply {
                        put("name", "save_note")
                        put("description", "Create and save a smart note with category")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("content", JSONObject().put("type", "string").put("description", "Content of the note"))
                                put("title", JSONObject().put("type", "string").put("description", "Short title"))
                                put("category", JSONObject().put("type", "string").put("description", "'Notes', 'Shopping', 'Ideas', 'College', 'Personal', 'Important'"))
                            })
                            put("required", JSONArray().put("content"))
                        })
                    })

                    // Shopping List Manager
                    put(JSONObject().apply {
                        put("name", "manage_shopping_list")
                        put("description", "Add, remove, mark purchased, or read shopping list items")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("action", JSONObject().put("type", "string").put("description", "'add', 'remove', 'mark_done', 'list'"))
                                put("items", JSONObject().put("type", "string").put("description", "Item names e.g. 'milk, bread, eggs'"))
                            })
                            put("required", JSONArray().put("action"))
                        })
                    })

                    // Time-Based Scheduled Action
                    put(JSONObject().apply {
                        put("name", "schedule_action")
                        put("description", "Schedule an action or reminder for a specific future time or delay")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("title", JSONObject().put("type", "string").put("description", "Reminder title"))
                                put("time_string", JSONObject().put("type", "string").put("description", "Target time e.g. '12:00 PM', 'in 30 minutes', 'tomorrow 8:00 AM'"))
                                put("task_type", JSONObject().put("type", "string").put("description", "'CALL_REMINDER', 'STUDY_REMINDER', 'CUSTOM_REMINDER'"))
                                put("target_contact", JSONObject().put("type", "string").put("description", "Contact name to call or message if applicable"))
                            })
                            put("required", JSONArray().put("title").put("time_string"))
                        })
                    })

                    // Study Timer
                    put(JSONObject().apply {
                        put("name", "start_study_timer")
                        put("description", "Start a focused study session timer with DND and automatic break")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("duration_minutes", JSONObject().put("type", "integer").put("description", "Duration in minutes (e.g. 45 or 120)"))
                                put("topic", JSONObject().put("type", "string").put("description", "Subject or topic name (e.g. Robotics, Math)"))
                            })
                            put("required", JSONArray().put("duration_minutes"))
                        })
                    })

                    // Camera Assistant
                    put(JSONObject().apply {
                        put("name", "camera_action")
                        put("description", "Take photo, take selfie, record video, or start camera timer")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("mode", JSONObject().put("type", "string").put("description", "'photo', 'selfie', 'video', 'timer_10s'"))
                            })
                            put("required", JSONArray().put("mode"))
                        })
                    })

                    // Web Search
                    put(JSONObject().apply {
                        put("name", "web_search")
                        put("description", "Search the web or look up information online")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("query", JSONObject().put("type", "string").put("description", "Search query"))
                            })
                            put("required", JSONArray().put("query"))
                        })
                    })

                    // Audit Log Query
                    put(JSONObject().apply {
                        put("name", "query_audit_log")
                        put("description", "Answer 'what did you do today?' with an itemized breakdown of assistant actions")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("time_frame", JSONObject().put("type", "string").put("description", "'today'"))
                            })
                        })
                    })

                    // Daily Planner Query
                    put(JSONObject().apply {
                        put("name", "query_daily_plan")
                        put("description", "Summarize the user's upcoming classes, reminders, alarms, and tasks for today or tomorrow")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("day", JSONObject().put("type", "string").put("description", "'today' or 'tomorrow'"))
                            })
                        })
                    })
                })
            })
        }
    }

    companion object {
        const val MODEL_NAME = "gemini-3.6-flash"
        const val DEFAULT_FALLBACK_KEY = ""
    }
}
