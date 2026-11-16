package com.example.ailearning.domain.usecase

import com.example.ailearning.domain.model.AnswerHistory
import com.example.ailearning.domain.model.CategoryStatistics
import com.example.ailearning.domain.model.DailyStatistics
import com.example.ailearning.domain.model.DifficultyStatistics
import com.example.ailearning.domain.model.LearningSession
import com.example.ailearning.domain.model.OverallStatistics
import com.example.ailearning.domain.model.StatisticsPeriod
import com.example.ailearning.domain.repository.AnswerHistoryRepository
import com.example.ailearning.domain.repository.LearningSessionRepository
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
