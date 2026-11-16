package com.example.ailearning.data.repository

import com.example.ailearning.data.local.db.LearningSessionDao
import com.example.ailearning.domain.model.LearningSession
import com.example.ailearning.domain.repository.LearningSessionRepository
import javax.inject.Inject

class LearningSessionRepositoryImpl @Inject constructor(
    private val dao: LearningSessionDao
) : LearningSessionRepository {
    override suspend fun insert(session: LearningSession) = Result.success(dao.insert(session))
    override suspend fun update(session: LearningSession) = Result.success(dao.update(session))
    override suspend fun getRecentByLearningSet(learningSetId: String, limit: Int) = Result.success(dao.getRecentByLearningSet(learningSetId, limit))
    override suspend fun getById(id: String) = Result.success(dao.getById(id))
}
