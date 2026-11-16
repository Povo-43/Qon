package com.example.ailearning.data.repository

import com.example.ailearning.data.local.db.AiAnalysisDao
import com.example.ailearning.domain.model.AiAnalysis
import com.example.ailearning.domain.repository.AiAnalysisRepository
import javax.inject.Inject

class AiAnalysisRepositoryImpl @Inject constructor(
    private val dao: AiAnalysisDao
) : AiAnalysisRepository {
    override suspend fun insert(analysis: AiAnalysis) = Result.success(dao.insert(analysis))
    override suspend fun getRecentByLearningSet(learningSetId: String, limit: Int) = Result.success(dao.getRecentByLearningSet(learningSetId, limit))
    override suspend fun getLatestByType(learningSetId: String, type: String) = Result.success(dao.getLatestByType(learningSetId, type))
}
