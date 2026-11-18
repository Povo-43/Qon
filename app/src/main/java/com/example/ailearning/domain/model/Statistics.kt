package com.example.ailearning.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OverallStatistics(
    val totalQuestions: Int,
    val totalCorrect: Int,
    val overallAccuracy: Double,
    val totalStudyTimeMs: Long,
    val avgResponseTimeMs: Double,
    val totalSessions: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int
) {
    fun getFormattedStudyTime(): String {
        val hours = totalStudyTimeMs / (1000 * 60 * 60)
        val minutes = (totalStudyTimeMs % (1000 * 60 * 60)) / (1000 * 60)
        return if (hours > 0) "${hours}時間${minutes}分" else "${minutes}分"
    }
    
    fun getFormattedAvgResponseTime(): String = String.format("%.1f秒", avgResponseTimeMs / 1000)
}

@Serializable
data class CategoryStatistics(
    val category: String,
    val total: Int,
    val correct: Int,
    val accuracy: Double,
    val avgResponseTimeMs: Double
) {
    fun getFormattedAvgResponseTime(): String = String.format("%.1f秒", avgResponseTimeMs / 1000)
}

@Serializable
data class DifficultyStatistics(
    val difficulty: Int,
    val total: Int,
    val correct: Int,
    val accuracy: Double,
    val avgResponseTimeMs: Double
) {
    fun getDifficultyLabel(): String = when (difficulty) {
        1 -> "とても易しい"
        2 -> "易しい"
        3 -> "普通"
        4 -> "難しい"
        5 -> "とても難しい"
        else -> "不明"
    }
    
    fun getFormattedAvgResponseTime(): String = String.format("%.1f秒", avgResponseTimeMs / 1000)
}

@Serializable
data class DailyStatistics(
    val date: String,  // YYYY-MM-DD
    val total: Int,
    val correct: Int,
    val accuracy: Double,
    val studyTimeMs: Long
) {
    fun getFormattedStudyTime(): String {
        val minutes = studyTimeMs / (1000 * 60)
        return if (minutes >= 60) "${minutes / 60}時間${minutes % 60}分" else "${minutes}分"
    }
}

enum class StatisticsPeriod {
    TODAY, WEEK, MONTH, ALL
}

/** 全セット横断の統括統計（v1.6.6） */
@Serializable
data class GlobalStatistics(
    val totalQuestions: Int,
    val totalCorrect: Int,
    val overallAccuracy: Double,
    val totalStudyTimeMs: Long,
    val avgResponseTimeMs: Double,
    val appTotal: Int,
    val appCorrect: Int,
    val widgetTotal: Int,
    val widgetCorrect: Int,
    val focusLockTotal: Int,
    val focusLockCorrect: Int,
    val focusUnlockCount: Int,
    val focusWrongCount: Int
) {
    fun getFormattedStudyTime(): String {
        val hours = totalStudyTimeMs / (1000 * 60 * 60)
        val minutes = (totalStudyTimeMs % (1000 * 60 * 60)) / (1000 * 60)
        return if (hours > 0) "${hours}時間${minutes}分" else "${minutes}分"
    }

    fun getFormattedAvgResponseTime(): String = String.format("%.1f秒", avgResponseTimeMs / 1000)

    fun getAppAccuracy(): Double = if (appTotal > 0) appCorrect.toDouble() / appTotal else 0.0
    fun getWidgetAccuracy(): Double = if (widgetTotal > 0) widgetCorrect.toDouble() / widgetTotal else 0.0
    fun getFocusLockAccuracy(): Double = if (focusLockTotal > 0) focusLockCorrect.toDouble() / focusLockTotal else 0.0

    /** ソース別ラベル付きリスト（エクスポート・表示用） */
    fun sourceBreakdown(): List<SourceBreakdown> = listOf(
        SourceBreakdown("アプリ学習", SOURCE_APP, appTotal, appCorrect, getAppAccuracy()),
        SourceBreakdown("ウィジェット", SOURCE_WIDGET, widgetTotal, widgetCorrect, getWidgetAccuracy()),
        SourceBreakdown("集中モード（アプリロック）", "focus_lock", focusLockTotal, focusLockCorrect, getFocusLockAccuracy())
    ).filter { it.total > 0 }
}

@Serializable
data class SourceBreakdown(
    val label: String,
    val source: String,
    val total: Int,
    val correct: Int,
    val accuracy: Double
)

const val SOURCE_APP = "app"
const val SOURCE_WIDGET = "widget"
const val SOURCE_FOCUS_LOCK = "focus_lock"

@Serializable
data class AiAnalysisResult(
    val summary: String,
    val weakCategories: List<WeakCategory>,
    val recommendations: List<String>,
    val nextFocusAreas: List<String>
)

@Serializable
data class WeakCategory(
    val category: String,
    val accuracy: Double,
    val totalAttempts: Int,
    val suggestion: String
)
