package com.aaya.assistant.engine.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object WebIntelligenceEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun getInstantAnswer(query: String): String? = withContext(Dispatchers.IO) {
        val lower = query.lowercase().trim()

        // 1. Weather Query ("Mausam kaisa hai", "Weather in Delhi")
        if (lower.contains("weather") || lower.contains("mausam") || lower.contains("temperature") || lower.contains("barish")) {
            val city = extractCity(query)
            return@withContext fetchFreeWeather(city)
        }

        // 2. DuckDuckGo Instant Knowledge Query
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                val abstractText = json.optString("AbstractText", "")
                if (abstractText.isNotBlank()) {
                    return@withContext abstractText
                }

                val answer = json.optString("Answer", "")
                if (answer.isNotBlank()) {
                    return@withContext answer
                }
            }
        } catch (e: Exception) {
            // Fall through to Gemini AI
        }

        return@withContext null
    }

    private fun fetchFreeWeather(city: String): String? {
        return try {
            val target = if (city.isNotBlank()) city else ""
            val url = "https://wttr.in/$target?format=%C+%t+(Humidity:+%h,+Wind:+%w)"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "curl/7.68.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val condition = response.body?.string()?.trim() ?: ""
                    if (condition.isNotEmpty() && !condition.contains("Unknown location")) {
                        "Current weather${if (city.isNotBlank()) " in $city" else ""}: $condition."
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractCity(query: String): String {
        val parts = query.split(" ")
        val inIdx = parts.indexOfFirst { it.equals("in", ignoreCase = true) || it.equals("me", ignoreCase = true) }
        return if (inIdx != -1 && inIdx < parts.size - 1) {
            parts[inIdx + 1].trim().filter { it.isLetter() }
        } else {
            ""
        }
    }
}
