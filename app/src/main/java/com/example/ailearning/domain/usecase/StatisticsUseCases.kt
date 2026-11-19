package com.example.ailearning.domain.usecase

import com.example.ailearning.domain.model.AnswerHistory
import com.example.ailearning.domain.model.CategoryStatistics
import com.example.ailearning.domain.model.DailyStatistics
import com.example.ailearning.domain.model.DifficultyStatistics
import com.example.ailearning.domain.model.GlobalStatistics
import com.example.ailearning.domain.model.LearningSession
import com.example.ailearning.domain.model.OverallStatistics
import com.example.ailearning.domain.model.StatisticsPeriod
import com.example.ailearning.data.local.prefs.SettingsPreferences
import com.example.ailearning.domain.repository.AnswerHistoryRepository
import com.example.ailearning.domain.repository.LearningSessionRepository
import com.example.ailearning.domain.repository.SourceCount
import javax.inject.Inject

class GetOverallStatisticsUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository,
    private val sessionRepository: LearningSessionRepository
) {
    operator suspend fun invoke(learningSetId: String, period: StatisticsPeriod): Result<OverallStatistics> {
        return try {
            val startTime = getStartTime(period)
            
            val totalQuestions = historyRepository.countByLearningSet(learningSetId).getOrElse { 0 }
            val accuracy = historyRepository.getAccuracyByLearningSet(learningSetId).getOrElse { 0.0 }
            val avgResponseTime = historyRepository.getAvgResponseTimeCorrect(learningSetId).getOrElse { 0.0 }
            
            val sessions = sessionRepository.getRecentByLearningSet(learningSetId, 100).getOrElse { emptyList() }
            val totalSessions = sessions.size
            val totalStudyTime = sessions.sumOf { it.totalTimeMs }
            
            // 連続学習日数計算
            val (currentStreak, longestStreak) = calculateStreaks(sessions)
            
            Result.success(OverallStatistics(
                totalQuestions = totalQuestions,
                totalCorrect = (totalQuestions * accuracy).toInt(),
                overallAccuracy = accuracy,
                totalStudyTimeMs = totalStudyTime,
                avgResponseTimeMs = avgResponseTime,
                totalSessions = totalSessions,
                currentStreakDays = currentStreak,
                longestStreakDays = longestStreak
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getStartTime(period: StatisticsPeriod): Long {
        val now = System.currentTimeMillis()
        return when (period) {
            StatisticsPeriod.TODAY -> now - 24 * 60 * 60 * 1000
            StatisticsPeriod.WEEK -> now - 7 * 24 * 60 * 60 * 1000
            StatisticsPeriod.MONTH -> now - 30 * 24 * 60 * 60 * 1000
            StatisticsPeriod.ALL -> 0
        }
    }
    
    private fun calculateStreaks(sessions: List<LearningSession>): Pair<Int, Int> {
        if (sessions.isEmpty()) return 0 to 0
        
        val dates = sessions.map { it.startedAt }.distinct().sortedDescending()
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0
        var lastDate: Long? = null
        
        for (timestamp in dates) {
            val date = timestamp / (24 * 60 * 60 * 1000)
            if (lastDate == null || lastDate == date + 1) {
                tempStreak++
            } else {
                tempStreak = 1
            }
            longestStreak = maxOf(longestStreak, tempStreak)
            lastDate = date
        }
        
        // 現在の連続日数（今日または昨日から続いているか）
        val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000)
        val yesterday = today - 1
        if (dates.isNotEmpty() && (dates[0] == today || dates[0] == yesterday)) {
            currentStreak = tempStreak
        }
        
        return currentStreak to longestStreak
    }
}

class GetCategoryStatisticsUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository
) {
    operator suspend fun invoke(learningSetId: String, period: StatisticsPeriod): Result<List<CategoryStatistics>> {
        return try {
            val startTime = getStartTime(period)
            val stats = historyRepository.getCategoryStats(learningSetId, startTime).getOrElse { emptyList() }
            
            Result.success(stats.map { CategoryStatistics(
                category = it.category,
                total = it.total,
                correct = it.correct,
                accuracy = it.accuracy,
                avgResponseTimeMs = it.avgResponseTimeMs
            ) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getStartTime(period: StatisticsPeriod): Long {
        val now = System.currentTimeMillis()
        return when (period) {
            StatisticsPeriod.TODAY -> now - 24 * 60 * 60 * 1000
            StatisticsPeriod.WEEK -> now - 7 * 24 * 60 * 60 * 1000
            StatisticsPeriod.MONTH -> now - 30 * 24 * 60 * 60 * 1000
            StatisticsPeriod.ALL -> 0
        }
    }
}

class GetDifficultyStatisticsUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository
) {
    operator suspend fun invoke(learningSetId: String, period: StatisticsPeriod): Result<List<DifficultyStatistics>> {
        return try {
            val startTime = getStartTime(period)
            val stats = historyRepository.getDifficultyStats(learningSetId, startTime).getOrElse { emptyList() }
            
            Result.success(stats.map { DifficultyStatistics(
                difficulty = it.difficulty,
                total = it.total,
                correct = it.correct,
                accuracy = it.accuracy,
                avgResponseTimeMs = it.avgResponseTimeMs
            ) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getStartTime(period: StatisticsPeriod): Long {
        val now = System.currentTimeMillis()
        return when (period) {
            StatisticsPeriod.TODAY -> now - 24 * 60 * 60 * 1000
            StatisticsPeriod.WEEK -> now - 7 * 24 * 60 * 60 * 1000
            StatisticsPeriod.MONTH -> now - 30 * 24 * 60 * 60 * 1000
            StatisticsPeriod.ALL -> 0
        }
    }
}

class GetLearningTrendUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository
) {
    operator suspend fun invoke(learningSetId: String, days: Int): Result<List<DailyStatistics>> {
        return try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - days * 24 * 60 * 60 * 1000L
            
            val histories = historyRepository.getByDateRange(learningSetId, startTime, endTime).getOrElse { emptyList() }
            
            val dailyMap = mutableMapOf<String, MutableList<AnswerHistory>>()
            for (h in histories) {
                val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(h.answeredAt))
                dailyMap.getOrPut(date) { mutableListOf() }.add(h)
            }
            
            val result = (0 until days).map { offset ->
                val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date(endTime - offset * 24 * 60 * 60 * 1000L))
                val dayHistories = dailyMap[date] ?: emptyList()
                val total = dayHistories.size
                val correct = dayHistories.count { it.isCorrect }
                val accuracy = if (total > 0) correct.toDouble() / total else 0.0
                val studyTime = dayHistories.sumOf { it.responseTimeMs }
                DailyStatistics(date, total, correct, accuracy, studyTime)
            }.reversed()
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 全セット横断の統括統計を取得する（v1.6.6）。
 * 集中モード（focus_lock）の解除回数・誤答回数も含める。
 */
class GetGlobalStatisticsUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository,
    private val settingsPreferences: com.example.ailearning.data.local.prefs.SettingsPreferences
) {
    operator suspend fun invoke(): Result<GlobalStatistics> {
        return try {
            val totalQuestions = historyRepository.countAll().getOrElse { 0 }
            val accuracy = historyRepository.getAccuracyAll().getOrElse { 0.0 }
            val avgResponseTime = historyRepository.getAvgResponseTimeCorrectAll().getOrElse { 0.0 }
            val totalTime = historyRepository.getTotalResponseTimeAll().getOrElse { 0L }

            val appStats = historyRepository.getCountBySource(SOURCE_APP).getOrElse { SourceCount(0, 0) }
            val widgetStats = historyRepository.getCountBySource(SOURCE_WIDGET).getOrElse { SourceCount(0, 0) }
            val focusStats = historyRepository.getCountBySource(SOURCE_FOCUS_LOCK).getOrElse { SourceCount(0, 0) }

            Result.success(GlobalStatistics(
                totalQuestions = totalQuestions,
                totalCorrect = (totalQuestions * accuracy).toInt(),
                overallAccuracy = accuracy,
                totalStudyTimeMs = totalTime,
                avgResponseTimeMs = avgResponseTime,
                appTotal = appStats.total,
                appCorrect = appStats.correct,
                widgetTotal = widgetStats.total,
                widgetCorrect = widgetStats.correct,
                focusLockTotal = focusStats.total,
                focusLockCorrect = focusStats.correct,
                focusUnlockCount = settingsPreferences.focusUnlockCount,
                focusWrongCount = settingsPreferences.focusWrongCount
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 統計を CSV 形式でエクスポートする（v1.6.6）。
 * キャッシュディレクトリに CSV ファイルを保存し、その File を返す。
 * 共有は ViewModel 側で FileProvider を経由して content Uri 化して行う。
 */
class ExportStatisticsUseCase @Inject constructor(
    private val context: android.content.Context,
    private val historyRepository: AnswerHistoryRepository
) {
    operator suspend fun invoke(): Result<java.io.File> {
        return try {
            val startTime = 0L
            val endTime = System.currentTimeMillis()
            val histories = historyRepository.getAllByDateRange(startTime, endTime).getOrElse { emptyList() }

            val csv = buildString {
                appendLine("id,questionId,learningSetId,selectedAnswer,isCorrect,responseTimeMs,answeredAt,sessionId,source")
                for (h in histories) {
                    append("${h.id},")
                    append("${h.questionId},")
                    append("${h.learningSetId},")
                    append("${h.selectedAnswer},")
                    append("${h.isCorrect},")
                    append("${h.responseTimeMs},")
                    append("${h.answeredAt},")
                    append("${h.sessionId},")
                    appendLine("${h.source}")
                }
            }

            val file = java.io.File(context.cacheDir, "qon_statistics_${System.currentTimeMillis()}.csv")
            file.writeText(csv)

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private const val SOURCE_APP = "app"
private const val SOURCE_WIDGET = "widget"
private const val SOURCE_FOCUS_LOCK = "focus_lock"
