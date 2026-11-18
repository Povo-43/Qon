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

    // ---- 集中モード（v1.6.0）----

    val focusEnabled: Boolean
        get() = prefs.getBoolean("focus_enabled", false)

    /** ゲートの問題を出す学習セット。空なら自動選択 */
    val focusLearningSetId: String
        get() = prefs.getString("focus_learning_set_id", "")!!

    /** 解除に必要な正解数（1〜5） */
    val focusRequiredCorrect: Int
        get() = prefs.getInt("focus_required_correct", 1).coerceIn(1, 5)

    /** 解除の有効期間（分）。0 = 次のロックまで */
    val focusUnlockDurationMinutes: Int
        get() = prefs.getInt("focus_unlock_duration_minutes", 0)

    /** 誤答時に正解と解説を表示するか */
    val focusShowCorrectAnswer: Boolean
        get() = prefs.getBoolean("focus_show_correct_answer", true)

    /** 解除セッション中か */
    val focusUnlocked: Boolean
        get() = prefs.getBoolean("focus_unlocked", false)

    /** 解除の有効期限。0 = 次のロックまで */
    val focusUnlockValidUntil: Long
        get() = prefs.getLong("focus_unlock_valid_until", 0L)

    /** 解除セッションが今も有効か（期限付き設定に対応） */
    fun isFocusUnlockSessionActive(): Boolean {
        if (!focusUnlocked) return false
        val until = focusUnlockValidUntil
        return until <= 0L || System.currentTimeMillis() < until
    }

    val focusUnlockedAt: Long
        get() = prefs.getLong("focus_unlocked_at", 0L)

    /** 累計解除回数 */
    val focusUnlockCount: Int
        get() = prefs.getInt("focus_unlock_count", 0)

    /** 累計誤答回数 */
    val focusWrongCount: Int
        get() = prefs.getInt("focus_wrong_count", 0)

    /** 最近ゲートで出題した問題ID（重複出題防止用・古い順） */
    fun getFocusRecentQuestionIdList(): List<String> =
        prefs.getString("focus_recent_question_ids", "")!!
            .split(",")
            .filter { it.isNotBlank() }

    fun getFocusRecentQuestionIds(): Set<String> = getFocusRecentQuestionIdList().toSet()

    fun appendFocusRecentQuestionId(questionId: String) {
        val updated = LinkedHashSet(getFocusRecentQuestionIds())
        updated.add(questionId)
        while (updated.size > RECENT_QUESTION_LIMIT) {
            updated.remove(updated.first())
        }
        prefs.edit().putString("focus_recent_question_ids", updated.joinToString(",")).apply()
    }

    fun setFocusEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("focus_enabled", enabled).apply()
    }

    fun setFocusLearningSetId(setId: String) {
        prefs.edit().putString("focus_learning_set_id", setId).apply()
    }

    fun setFocusRequiredCorrect(count: Int) {
        prefs.edit().putInt("focus_required_correct", count.coerceIn(1, 5)).apply()
    }

    fun setFocusUnlockDurationMinutes(minutes: Int) {
        prefs.edit().putInt("focus_unlock_duration_minutes", minutes).apply()
    }

    fun setFocusShowCorrectAnswer(show: Boolean) {
        prefs.edit().putBoolean("focus_show_correct_answer", show).apply()
    }

    fun setFocusUnlocked(unlocked: Boolean, validUntil: Long = 0L) {
        prefs.edit()
            .putBoolean("focus_unlocked", unlocked)
            .putLong("focus_unlock_valid_until", validUntil)
            .putLong("focus_unlocked_at", System.currentTimeMillis())
            .apply()
    }

    fun incrementFocusUnlockCount() {
        prefs.edit().putInt("focus_unlock_count", focusUnlockCount + 1).apply()
    }

    fun incrementFocusWrongCount() {
        prefs.edit().putInt("focus_wrong_count", focusWrongCount + 1).apply()
    }

    companion object {
        /** 重複出題防止で保持する最近の問題ID数 */
        const val RECENT_QUESTION_LIMIT = 30
    }
}
