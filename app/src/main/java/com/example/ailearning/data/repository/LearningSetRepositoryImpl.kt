package com.example.ailearning.data.repository

import com.example.ailearning.data.local.db.LearningSetDao
import com.example.ailearning.domain.model.LearningSet
import com.example.ailearning.domain.repository.LearningSetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LearningSetRepositoryImpl @Inject constructor(
    private val dao: LearningSetDao
) : LearningSetRepository {
    override suspend fun insert(set: LearningSet) = Result.success(dao.insert(set))
    override suspend fun update(set: LearningSet) = Result.success(dao.update(set))
    override suspend fun delete(set: LearningSet) = Result.success(dao.delete(set))
    override suspend fun deleteById(id: String) = Result.success(dao.deleteById(id))
    override fun getAllActive(): Flow<List<LearningSet>> = dao.getAllActive()
    override fun getById(id: String): Flow<LearningSet?> = dao.getById(id)
    override suspend fun getByIdOnce(id: String) = Result.success(dao.getByIdOnce(id))
}
