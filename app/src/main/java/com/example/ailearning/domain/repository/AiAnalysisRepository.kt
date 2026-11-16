package com.example.ailearning.domain.repository

import com.example.ailearning.domain.model.AiAnalysis

interface AiAnalysisRepository {
    suspend fun insert(analysis: AiAnalysis): Result<Long>
    suspend fun getRecentByLearningSet(learningSetId: String, limit: Int): Result<List<AiAnalysis>>
    suspend fun getLatestByType(learningSetId: String, type: String): Result<AiAnalysis?>
}
