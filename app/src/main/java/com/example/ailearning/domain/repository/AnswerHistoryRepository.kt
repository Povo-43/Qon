package com.example.ailearning.domain.repository

import com.example.ailearning.domain.model.AnswerHistory
import com.example.ailearning.domain.model.CategoryStatistics
import com.example.ailearning.domain.model.DifficultyStatistics

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
}
