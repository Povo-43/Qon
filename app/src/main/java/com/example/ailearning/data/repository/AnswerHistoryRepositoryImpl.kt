package com.example.ailearning.data.repository

import com.example.ailearning.data.local.db.AnswerHistoryDao
import com.example.ailearning.domain.model.AnswerHistory
import com.example.ailearning.domain.model.CategoryStatistics
import com.example.ailearning.domain.model.DifficultyStatistics
import com.example.ailearning.domain.repository.AnswerHistoryRepository
import javax.inject.Inject

class AnswerHistoryRepositoryImpl @Inject constructor(
    private val dao: AnswerHistoryDao
) : AnswerHistoryRepository {
    override suspend fun insert(history: AnswerHistory) = Result.success(dao.insert(history))
    override suspend fun insertAll(histories: List<AnswerHistory>) = Result.success(dao.insertAll(histories))
    override suspend fun getRecentByLearningSet(learningSetId: String, limit: Int) = Result.success(dao.getRecentByLearningSet(learningSetId, limit))
    override suspend fun getByQuestionId(questionId: String) = Result.success(dao.getByQuestionId(questionId))
    override suspend fun getBySessionId(sessionId: String) = Result.success(dao.getBySessionId(sessionId))
    override suspend fun getByDateRange(learningSetId: String, startTime: Long, endTime: Long) = Result.success(dao.getByDateRange(learningSetId, startTime, endTime))
    override suspend fun countByLearningSet(learningSetId: String) = Result.success(dao.countByLearningSet(learningSetId))
    override suspend fun getAccuracyByLearningSet(learningSetId: String) = Result.success(dao.getAccuracyByLearningSet(learningSetId))
    override suspend fun getAvgResponseTimeCorrect(learningSetId: String) = Result.success(dao.getAvgResponseTimeCorrect(learningSetId))
    override suspend fun getCategoryStats(learningSetId: String, startTime: Long) = Result.success(
        dao.getCategoryStats(learningSetId, startTime).map { 
            com.example.ailearning.domain.model.CategoryStatistics(
                category = it.category,
                total = it.total,
                correct = it.correct,
                accuracy = it.accuracy,
                avgResponseTimeMs = it.avgTime
            ) 
        }
    )
    override suspend fun getDifficultyStats(learningSetId: String, startTime: Long) = Result.success(
        dao.getDifficultyStats(learningSetId, startTime).map { 
            com.example.ailearning.domain.model.DifficultyStatistics(
                difficulty = it.difficulty,
                total = it.total,
                correct = it.correct,
                accuracy = it.accuracy,
                avgResponseTimeMs = it.avgTime
            ) 
        }
    )
}
