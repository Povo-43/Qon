package com.example.ailearning.domain.repository

import com.example.ailearning.domain.model.LearningSession

interface LearningSessionRepository {
    suspend fun insert(session: LearningSession): Result<Long>
    suspend fun update(session: LearningSession): Result<Int>
    suspend fun getRecentByLearningSet(learningSetId: String, limit: Int): Result<List<LearningSession>>
    suspend fun getById(id: String): Result<LearningSession?>
}
