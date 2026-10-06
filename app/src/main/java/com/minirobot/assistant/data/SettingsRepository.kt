package com.minirobot.assistant.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Repository to persist user preferences, AI provider settings, and API keys.
 * API keys are stored locally on-device in private SharedPreferences and never committed.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()

    var aiProviderMode: String
        get() = prefs.getString(KEY_AI_PROVIDER_MODE, PROVIDER_LOCAL) ?: PROVIDER_LOCAL
        set(value) = prefs.edit().putString(KEY_AI_PROVIDER_MODE, value).apply()

    var geminiModel: String
        get() = prefs.getString(KEY_GEMINI_MODEL, "gemini-1.5-flash") ?: "gemini-1.5-flash"
        set(value) = prefs.edit().putString(KEY_GEMINI_MODEL, value).apply()

    var isTtsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()

    var isOverlayServiceRunning: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_RUNNING, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_RUNNING, value).apply()

    companion object {
        private const val PREFS_NAME = "mini_robot_assistant_prefs"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_AI_PROVIDER_MODE = "ai_provider_mode"
        private const val KEY_GEMINI_MODEL = "gemini_model"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_OVERLAY_RUNNING = "overlay_running"

        const val PROVIDER_LOCAL = "LOCAL_OFFLINE"
        const val PROVIDER_GEMINI = "GEMINI"
    }
}
