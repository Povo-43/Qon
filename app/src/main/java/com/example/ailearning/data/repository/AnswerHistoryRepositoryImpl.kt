package com.example.ailearning.data.repository

import com.example.ailearning.data.local.db.AnswerHistoryDao
import com.example.ailearning.domain.model.AnswerHistory
import com.example.ailearning.domain.model.CategoryStatistics
import com.example.ailearning.domain.model.DifficultyStatistics
import com.example.ailearning.domain.repository.AnswerHistoryRepository
import com.example.ailearning.domain.repository.DailyStatRow
import com.example.ailearning.domain.repository.SourceCount
import com.example.ailearning.domain.repository.SourceStat
import javax.inject.Inject

class AnswerHistoryRepositoryImpl @Inject constructor(
    private val dao: AnswerHistoryDao
) : AnswerHistoryRepository {
    override suspend fun insert(history: AnswerHistory) = Result.success(dao.insert(history))
    override suspend fun insertAll(histories: List<AnswerHistory>) = Result.success(dao.insertAll(histories))
    override suspend fun getRecentByLearningSet(learningSetId: String, limit: Int) = Result.success(dao.getRecentByLearningSet(learningSetId, limit))
    override suspend fun getByQuestionId(questionId: String) = Result.success(dao.getByQuestionId(questionId))
    override suspend fun getBySessionId(sessionId: String) = Result.success(dao.getBySessionId(sessionId))
    override suspend fun getAnsweredQuestionIdsBySession(sessionId: String) = Result.success(dao.getAnsweredQuestionIdsBySession(sessionId))
    override suspend fun getAnsweredQuestionIdsByLearningSet(learningSetId: String) = Result.success(dao.getAnsweredQuestionIdsByLearningSet(learningSetId))
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

    // ---- 全セット横断統計（v1.6.6）----
    override suspend fun countAll() = Result.success(dao.countAll())
    override suspend fun getAccuracyAll() = Result.success(dao.getAccuracyAll())
    override suspend fun getAvgResponseTimeCorrectAll() = Result.success(dao.getAvgResponseTimeCorrectAll())
    override suspend fun getTotalResponseTimeAll() = Result.success(dao.getTotalResponseTimeAll())
    override suspend fun getStatsBySource() = Result.success(
        dao.getStatsBySource().map {
            SourceStat(source = it.source, total = it.total, correct = it.correct, avgTime = it.avgTime)
        }
    )
    override suspend fun getCountBySource(source: String) = Result.success(
        SourceCount(total = dao.getCountBySource(source).total, correct = dao.getCountBySource(source).correct)
    )
    override suspend fun getAllByDateRange(startTime: Long, endTime: Long) = Result.success(dao.getAllByDateRange(startTime, endTime))
    override suspend fun getDailyStatsAll() = Result.success(
        dao.getDailyStatsAll().map {
            DailyStatRow(date = it.date, total = it.total, correct = it.correct, avgTime = it.avgTime, studyTime = it.studyTime)
        }
    )

    // ---- 全セット横断分野別統計（v1.6.10）----
    override suspend fun getCategoryStatsAll(startTime: Long) = Result.success(
        dao.getCategoryStatsAll(startTime).map {
            com.example.ailearning.domain.model.CategoryStatistics(
                category = it.category,
                total = it.total,
                correct = it.correct,
                accuracy = if (it.total > 0) it.correct.toDouble() / it.total else 0.0,
                avgResponseTimeMs = it.avgTime
            )
        }
    )
}
