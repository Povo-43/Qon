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

    // ---- アプリごとの解除セッション（v1.6.3）----
    // 「パッケージ名|有効期限」をカンマ区切りで保持する。
    // 期限 0 は「次のロックまで」、正解はアプリごとに独立して有効。

    /** 現在解除されているアプリと有効期限の一覧 */
    fun getFocusUnlockedPackages(): MutableMap<String, Long> =
        parseUnlockedPackages(prefs.getString("focus_unlocked_packages", "")!!)

    fun setFocusUnlockedPackages(map: Map<String, Long>) {
        prefs.edit().putString("focus_unlocked_packages", serializeUnlockedPackages(map)).apply()
    }

    /** 指定したアプリの解除セッションを開始する。validUntil = 0 は次のロックまで */
    fun setPackageUnlocked(packageName: String, validUntil: Long) {
        val map = getFocusUnlockedPackages()
        map[packageName] = validUntil
        setFocusUnlockedPackages(map)
        prefs.edit().putLong("focus_unlocked_at", System.currentTimeMillis()).apply()
    }

    /** そのアプリが今解除されているか（正解済み・有効期限内） */
    fun isPackageUnlocked(packageName: String): Boolean {
        val until = getFocusUnlockedPackages()[packageName] ?: return false
        return until <= 0L || System.currentTimeMillis() < until
    }

    /** すべての解除セッションを消す（「次のロックまで」モードのロック解除時など） */
    fun clearFocusUnlockedPackages() {
        prefs.edit().putString("focus_unlocked_packages", "").apply()
    }

    /** 期限切れの解除セッションを掃除する */
    fun clearExpiredFocusUnlocks(now: Long = System.currentTimeMillis()) {
        val map = getFocusUnlockedPackages()
        val cleaned = map.filterValues { it <= 0L || it > now }
        if (cleaned.size != map.size) setFocusUnlockedPackages(cleaned)
    }

    val focusUnlockedAt: Long
        get() = prefs.getLong("focus_unlocked_at", 0L)

    /** 問題の自動補充: この件数を下回る学習セットを裏で生成する。0 = 自動補充しない */
    val prefetchTargetCount: Int
        get() = prefs.getInt("prefetch_target_count", 50)

    fun setPrefetchTargetCount(count: Int) {
        prefs.edit().putInt("prefetch_target_count", count.coerceIn(0, 500)).apply()
    }

    private fun parseUnlockedPackages(raw: String): MutableMap<String, Long> {
        val map = LinkedHashMap<String, Long>()
        raw.split(",").forEach { entry ->
            val index = entry.lastIndexOf('|')
            if (index <= 0) return@forEach
            val pkg = entry.substring(0, index)
            val until = entry.substring(index + 1).toLongOrNull() ?: return@forEach
            if (pkg.isNotBlank()) map[pkg] = until
        }
        return map
    }

    private fun serializeUnlockedPackages(map: Map<String, Long>): String =
        map.entries.joinToString(",") { "${it.key}|${it.value}" }

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
