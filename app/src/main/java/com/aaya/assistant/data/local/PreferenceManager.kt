package com.aaya.assistant.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class PreferenceManager(context: Context) {

    private val securePrefs: SharedPreferences = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "aaya_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        // Fallback for older test environments or devices with Keystore exceptions
        context.getSharedPreferences("aaya_standard_prefs", Context.MODE_PRIVATE)
    }

    var apiKey: String
        get() = securePrefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = securePrefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var assistantName: String
        get() = securePrefs.getString(KEY_ASSISTANT_NAME, "AAYA") ?: "AAYA"
        set(value) = securePrefs.edit().putString(KEY_ASSISTANT_NAME, value).apply()

    var isShakeEnabled: Boolean
        get() = securePrefs.getBoolean(KEY_SHAKE_ENABLED, true)
        set(value) = securePrefs.edit().putBoolean(KEY_SHAKE_ENABLED, value).apply()

    var shakeSensitivity: Float
        get() = securePrefs.getFloat(KEY_SHAKE_SENSITIVITY, 13.0f)
        set(value) = securePrefs.edit().putFloat(KEY_SHAKE_SENSITIVITY, value).apply()

    var isAutoSleepEnabled: Boolean
        get() = securePrefs.getBoolean(KEY_AUTO_SLEEP, true)
        set(value) = securePrefs.edit().putBoolean(KEY_AUTO_SLEEP, value).apply()

    var isDrivingModeAuto: Boolean
        get() = securePrefs.getBoolean(KEY_DRIVING_AUTO, true)
        set(value) = securePrefs.edit().putBoolean(KEY_DRIVING_AUTO, value).apply()

    var totalApiRequests: Int
        get() = securePrefs.getInt(KEY_TOTAL_API_REQUESTS, 0)
        set(value) = securePrefs.edit().putInt(KEY_TOTAL_API_REQUESTS, value).apply()

    var successfulApiRequests: Int
        get() = securePrefs.getInt(KEY_SUCCESS_API_REQUESTS, 0)
        set(value) = securePrefs.edit().putInt(KEY_SUCCESS_API_REQUESTS, value).apply()

    var lastApiLatencyMs: Long
        get() = securePrefs.getLong(KEY_LAST_LATENCY_MS, 0L)
        set(value) = securePrefs.edit().putLong(KEY_LAST_LATENCY_MS, value).apply()

    var lastApiError: String?
        get() = securePrefs.getString(KEY_LAST_ERROR, null)
        set(value) = securePrefs.edit().putString(KEY_LAST_ERROR, value).apply()

    var userName: String
        get() = securePrefs.getString(KEY_USER_NAME, "Manish") ?: "Manish"
        set(value) = securePrefs.edit().putString(KEY_USER_NAME, value.trim()).apply()

    var voicePitch: Float
        get() = securePrefs.getFloat(KEY_VOICE_PITCH, 1.0f)
        set(value) = securePrefs.edit().putFloat(KEY_VOICE_PITCH, value).apply()

    var voiceSpeed: Float
        get() = securePrefs.getFloat(KEY_VOICE_SPEED, 1.0f)
        set(value) = securePrefs.edit().putFloat(KEY_VOICE_SPEED, value).apply()

    var voicePreset: String
        get() = securePrefs.getString(KEY_VOICE_PRESET, "FEMALE") ?: "FEMALE"
        set(value) = securePrefs.edit().putString(KEY_VOICE_PRESET, value).apply()

    var aiProvider: String
        get() = securePrefs.getString(KEY_AI_PROVIDER, "GEMINI") ?: "GEMINI"
        set(value) = securePrefs.edit().putString(KEY_AI_PROVIDER, value).apply()

    var openRouterApiKey: String
        get() = securePrefs.getString(KEY_OPENROUTER_KEY, "") ?: ""
        set(value) = securePrefs.edit().putString(KEY_OPENROUTER_KEY, value.trim()).apply()

    var isWaterReminderEnabled: Boolean
        get() = securePrefs.getBoolean(KEY_WATER_REMINDER, true)
        set(value) = securePrefs.edit().putBoolean(KEY_WATER_REMINDER, value).apply()

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_ASSISTANT_NAME = "assistant_name"
        private const val KEY_SHAKE_ENABLED = "shake_enabled"
        private const val KEY_SHAKE_SENSITIVITY = "shake_sensitivity"
        private const val KEY_AUTO_SLEEP = "auto_sleep"
        private const val KEY_DRIVING_AUTO = "driving_auto"
        private const val KEY_TOTAL_API_REQUESTS = "total_api_requests"
        private const val KEY_SUCCESS_API_REQUESTS = "success_api_requests"
        private const val KEY_LAST_LATENCY_MS = "last_latency_ms"
        private const val KEY_LAST_ERROR = "last_error"

        private const val KEY_USER_NAME = "user_name"
        private const val KEY_VOICE_PITCH = "voice_pitch"
        private const val KEY_VOICE_SPEED = "voice_speed"
        private const val KEY_VOICE_PRESET = "voice_preset"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_OPENROUTER_KEY = "openrouter_api_key"
        private const val KEY_WATER_REMINDER = "water_reminder_enabled"
    }
}
