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
