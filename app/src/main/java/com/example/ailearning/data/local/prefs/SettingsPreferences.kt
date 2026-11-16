package com.example.ailearning.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject

class SettingsPreferences @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val theme: String
        get() = prefs.getString("theme", "system")!!
    val autoGenerateQuestions: Boolean
        get() = prefs.getBoolean("auto_generate_questions", true)
    val questionsPerSession: Int
        get() = prefs.getInt("questions_per_session", 10)
    val widgetUpdateInterval: Int
        get() = prefs.getInt("widget_update_interval", 30)
    val showExplanation: Boolean
        get() = prefs.getBoolean("show_explanation", true)
    val hapticFeedback: Boolean
        get() = prefs.getBoolean("haptic_feedback", true)
    val soundEffects: Boolean
        get() = prefs.getBoolean("sound_effects", false)

    fun setTheme(theme: String) {
        prefs.edit().putString("theme", theme).apply()
    }

    fun setAutoGenerateQuestions(enabled: Boolean) {
        prefs.edit().putBoolean("auto_generate_questions", enabled).apply()
    }

    fun setQuestionsPerSession(count: Int) {
        prefs.edit().putInt("questions_per_session", count.coerceIn(5, 50)).apply()
    }

    fun setWidgetUpdateInterval(minutes: Int) {
        prefs.edit().putInt("widget_update_interval", minutes.coerceIn(5, 1440)).apply()
    }

    fun setShowExplanation(show: Boolean) {
        prefs.edit().putBoolean("show_explanation", show).apply()
    }

    fun setHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback", enabled).apply()
    }

    fun setSoundEffects(enabled: Boolean) {
        prefs.edit().putBoolean("sound_effects", enabled).apply()
    }
}
