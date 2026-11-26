package com.example.ailearning.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ailearning.domain.model.Question
import com.example.ailearning.domain.model.QuestionWithStats
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: Question): Long

    @Update
    suspend fun update(question: Question): Int

    @Delete
    suspend fun delete(question: Question): Int

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND isActive = 1 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestions(learningSetId: String, limit: Int): List<Question>

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND isActive = 1 AND id NOT IN (:excludeIds) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestions(learningSetId: String, limit: Int, excludeIds: List<String>): List<Question>

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND isActive = 1 AND category = :category ORDER BY RANDOM() LIMIT :limit")
    suspend fun getQuestionsByCategory(learningSetId: String, category: String, limit: Int): List<Question>

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND isActive = 1 AND category = :category AND id NOT IN (:excludeIds) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getQuestionsByCategory(learningSetId: String, category: String, limit: Int, excludeIds: List<String>): List<Question>

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND isActive = 1 AND difficulty = :difficulty ORDER BY RANDOM() LIMIT :limit")
    suspend fun getQuestionsByDifficulty(learningSetId: String, difficulty: Int, limit: Int): List<Question>

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND isActive = 1 AND difficulty = :difficulty AND id NOT IN (:excludeIds) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getQuestionsByDifficulty(learningSetId: String, difficulty: Int, limit: Int, excludeIds: List<String>): List<Question>

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND isActive = 1 ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentQuestions(learningSetId: String, limit: Int): List<Question>

    @Query("""
        SELECT q.* FROM questions q
        JOIN (
            SELECT questionId, AVG(CASE WHEN isCorrect THEN 1.0 ELSE 0.0 END) as accuracy
            FROM answer_histories
            WHERE learningSetId = :learningSetId
            GROUP BY questionId
            HAVING COUNT(*) >= 3 AND accuracy < 0.6
        ) ah ON q.id = ah.questionId
        WHERE q.learningSetId = :learningSetId AND q.isActive = 1
        ORDER BY ah.accuracy ASC
        LIMIT :limit
    """)
    suspend fun getWeakQuestions(learningSetId: String, limit: Int): List<Question>

    @Query("""
        SELECT q.* FROM questions q
        JOIN (
            SELECT questionId, AVG(CASE WHEN isCorrect THEN 1.0 ELSE 0.0 END) as accuracy
            FROM answer_histories
            WHERE learningSetId = :learningSetId
            GROUP BY questionId
            HAVING COUNT(*) >= 3 AND accuracy < 0.6
        ) ah ON q.id = ah.questionId
        WHERE q.learningSetId = :learningSetId AND q.isActive = 1 AND q.id NOT IN (:excludeIds)
        ORDER BY ah.accuracy ASC
        LIMIT :limit
    """)
    suspend fun getWeakQuestions(learningSetId: String, limit: Int, excludeIds: List<String>): List<Question>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: String): Question?

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND isActive = 1")
    fun getAllByLearningSet(learningSetId: String): Flow<List<Question>>

    @Query("SELECT COUNT(*) FROM questions WHERE learningSetId = :learningSetId AND isActive = 1")
    suspend fun countByLearningSet(learningSetId: String): Int

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND contentHash = :hash AND isActive = 1")
    suspend fun findByContentHash(learningSetId: String, hash: String): Question?

    // ---- 問題 + 回答統計（正答率表示・出題選択用）----

    /** 学習セットの全問題に回答統計を付けて返す（問題一覧ページ用・作成順）。 */
    @Query("""
        SELECT q.*, COALESCE(s.attempts, 0) AS attempts, COALESCE(s.correct, 0) AS correct
        FROM questions q
        LEFT JOIN (
            SELECT questionId,
                   COUNT(*) AS attempts,
                   SUM(CASE WHEN isCorrect THEN 1 ELSE 0 END) AS correct
            FROM answer_histories
            WHERE learningSetId = :learningSetId
            GROUP BY questionId
        ) s ON s.questionId = q.id
        WHERE q.learningSetId = :learningSetId AND q.isActive = 1
        ORDER BY q.createdAt DESC
    """)
    fun observeQuestionsWithStats(learningSetId: String): Flow<List<QuestionWithStats>>

    /** ランダム順で回答統計付きの問題を返す（出題選択用）。 */
    @Query("""
        SELECT q.*, COALESCE(s.attempts, 0) AS attempts, COALESCE(s.correct, 0) AS correct
        FROM questions q
        LEFT JOIN (
            SELECT questionId,
                   COUNT(*) AS attempts,
                   SUM(CASE WHEN isCorrect THEN 1 ELSE 0 END) AS correct
            FROM answer_histories
            WHERE learningSetId = :learningSetId
            GROUP BY questionId
        ) s ON s.questionId = q.id
        WHERE q.learningSetId = :learningSetId AND q.isActive = 1
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    suspend fun getQuestionsWithStats(learningSetId: String, limit: Int): List<QuestionWithStats>
}
