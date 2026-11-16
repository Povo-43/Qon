package com.example.ailearning.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ailearning.domain.model.LearningSession

@Dao
interface LearningSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: LearningSession): Long

    @Update
    suspend fun update(session: LearningSession): Int

    @Query("SELECT * FROM learning_sessions WHERE learningSetId = :learningSetId ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentByLearningSet(learningSetId: String, limit: Int): List<LearningSession>

    @Query("SELECT * FROM learning_sessions WHERE id = :id")
    suspend fun getById(id: String): LearningSession?
}
