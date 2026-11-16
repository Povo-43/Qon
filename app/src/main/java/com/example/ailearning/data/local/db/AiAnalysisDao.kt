package com.example.ailearning.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ailearning.domain.model.AiAnalysis

@Dao
interface AiAnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: AiAnalysis): Long

    @Query("SELECT * FROM ai_analyses WHERE learningSetId = :learningSetId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentByLearningSet(learningSetId: String, limit: Int): List<AiAnalysis>

    @Query("SELECT * FROM ai_analyses WHERE learningSetId = :learningSetId AND analysisType = :type ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByType(learningSetId: String, type: String): AiAnalysis?
}
