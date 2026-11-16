package com.example.ailearning.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ailearning.domain.model.AnswerHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: AnswerHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<AnswerHistory>): List<Long>

    @Query("SELECT * FROM answer_histories WHERE learningSetId = :learningSetId ORDER BY answeredAt DESC LIMIT :limit")
    suspend fun getRecentByLearningSet(learningSetId: String, limit: Int): List<AnswerHistory>

    @Query("SELECT * FROM answer_histories WHERE questionId = :questionId ORDER BY answeredAt DESC")
    suspend fun getByQuestionId(questionId: String): List<AnswerHistory>

    @Query("SELECT * FROM answer_histories WHERE sessionId = :sessionId ORDER BY answeredAt ASC")
    suspend fun getBySessionId(sessionId: String): List<AnswerHistory>

    @Query("SELECT * FROM answer_histories WHERE learningSetId = :learningSetId AND answeredAt >= :startTime AND answeredAt <= :endTime")
    suspend fun getByDateRange(learningSetId: String, startTime: Long, endTime: Long): List<AnswerHistory>

    @Query("SELECT COUNT(*) FROM answer_histories WHERE learningSetId = :learningSetId")
    suspend fun countByLearningSet(learningSetId: String): Int

    @Query("SELECT SUM(CASE WHEN isCorrect THEN 1 ELSE 0 END) * 1.0 / COUNT(*) FROM answer_histories WHERE learningSetId = :learningSetId")
    suspend fun getAccuracyByLearningSet(learningSetId: String): Double

    @Query("SELECT AVG(responseTimeMs) FROM answer_histories WHERE learningSetId = :learningSetId AND isCorrect = 1")
    suspend fun getAvgResponseTimeCorrect(learningSetId: String): Double

    @Query("""
        SELECT q.category as category, 
               COUNT(ah.id) as total,
               SUM(CASE WHEN ah.isCorrect THEN 1 ELSE 0 END) as correct,
               AVG(ah.responseTimeMs) as avgTime
        FROM answer_histories ah
        JOIN questions q ON ah.questionId = q.id
        WHERE ah.learningSetId = :learningSetId AND ah.answeredAt >= :startTime
        GROUP BY q.category
    """)
    suspend fun getCategoryStats(learningSetId: String, startTime: Long): List<CategoryStat>

    @Query("""
        SELECT q.difficulty as difficulty,
               COUNT(ah.id) as total,
               SUM(CASE WHEN ah.isCorrect THEN 1 ELSE 0 END) as correct,
               AVG(ah.responseTimeMs) as avgTime
        FROM answer_histories ah
        JOIN questions q ON ah.questionId = q.id
        WHERE ah.learningSetId = :learningSetId AND ah.answeredAt >= :startTime
        GROUP BY q.difficulty
        ORDER BY q.difficulty
    """)
    suspend fun getDifficultyStats(learningSetId: String, startTime: Long): List<DifficultyStat>
}
