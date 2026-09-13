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
    } catch (e: Exception) {
        // Fallback for older test environments or devices without Hardware Keystore
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
    }
}
