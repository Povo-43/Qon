package com.example.ailearning.domain.repository

import com.example.ailearning.domain.model.AnswerHistory
import com.example.ailearning.domain.model.CategoryStatistics
import com.example.ailearning.domain.model.DifficultyStatistics
import javax.inject.Inject

interface AnswerHistoryRepository {
    suspend fun insert(history: AnswerHistory): Result<Long>
    suspend fun insertAll(histories: List<AnswerHistory>): Result<List<Long>>
    suspend fun getRecentByLearningSet(learningSetId: String, limit: Int): Result<List<AnswerHistory>>
    suspend fun getByQuestionId(questionId: String): Result<List<AnswerHistory>>
    suspend fun getBySessionId(sessionId: String): Result<List<AnswerHistory>>
    suspend fun getByDateRange(learningSetId: String, startTime: Long, endTime: Long): Result<List<AnswerHistory>>
    suspend fun countByLearningSet(learningSetId: String): Result<Int>
    suspend fun getAccuracyByLearningSet(learningSetId: String): Result<Double>
    suspend fun getAvgResponseTimeCorrect(learningSetId: String): Result<Double>
    suspend fun getCategoryStats(learningSetId: String, startTime: Long): Result<List<CategoryStatistics>>
    suspend fun getDifficultyStats(learningSetId: String, startTime: Long): Result<List<DifficultyStatistics>>

    // ---- 全セット横断統計（v1.6.6）----
    suspend fun countAll(): Result<Int>
    suspend fun getAccuracyAll(): Result<Double>
    suspend fun getAvgResponseTimeCorrectAll(): Result<Double>
    suspend fun getTotalResponseTimeAll(): Result<Long>
    suspend fun getStatsBySource(): Result<List<SourceStat>>
    suspend fun getCountBySource(source: String): Result<SourceCount>
    suspend fun getAllByDateRange(startTime: Long, endTime: Long): Result<List<AnswerHistory>>
    suspend fun getDailyStatsAll(): Result<List<DailyStatRow>>

    // ---- 全セット横断分野別統計（v1.6.10）----
    suspend fun getCategoryStatsAll(startTime: Long): Result<List<CategoryStatistics>>
}

/** 全セット横断のソース別統計 */
data class SourceStat(
    val source: String,
    val total: Int,
    val correct: Int,
    val avgTime: Double
)

/** 全セット横断のソース別カウント */
data class SourceCount(
    val total: Int,
    val correct: Int
)

/** 全セット横断の日別統計 */
data class DailyStatRow(
    val date: String,
    val total: Int,
    val correct: Int,
    val avgTime: Double,
    val studyTime: Long
)
