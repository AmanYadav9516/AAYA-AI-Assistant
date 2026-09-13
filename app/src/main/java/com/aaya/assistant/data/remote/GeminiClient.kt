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
        val startTime = System.currentTimeMillis()

        try {
            val rootJson = JSONObject().apply {
                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", buildSystemPrompt(systemContext)))
                    })
                })

                // Contents
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", userQuery))
                        })
                    })
                })

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

                GeminiResult.Success(
                    responseText = textBuilder.toString().trim(),
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

        return if (apiKey.startsWith("AIzaSy")) {
            // Standard Google AI Studio Gemini API Key via query param
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
        } else {
            // OAuth, Bearer token, or direct token
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
            Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()
        }
    }

    private fun buildSystemPrompt(userContext: String): String {
        return """
            You are AAYA, a world-class voice assistant for Android.
            Your answers are concise, friendly, and optimized for voice synthesis (TTS).
            Never reply with long paragraphs. Keep voice responses under 1-2 sentences.
            Understand multilingual phrases in English, Hindi, and Hinglish (e.g., 'Mummy ko call karo', 'Papa ko phone lagao', 'Silent mode on karo').
            When a user requests a device action (calling, opening apps, setting alarms, turning on flashlight, sleep mode), always call the appropriate tool.
            
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
                })
            })
        }
    }

    companion object {
        const val DEFAULT_FALLBACK_KEY = ""
    }
}
