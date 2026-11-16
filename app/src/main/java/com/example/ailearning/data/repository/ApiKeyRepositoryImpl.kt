package com.example.ailearning.data.repository

import com.example.ailearning.data.local.db.ApiKeyDao
import com.example.ailearning.domain.model.ApiKey
import com.example.ailearning.domain.repository.ApiKeyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ApiKeyRepositoryImpl @Inject constructor(
    private val dao: ApiKeyDao
) : ApiKeyRepository {
    override suspend fun insert(key: ApiKey) = Result.success(dao.insert(key))
    override suspend fun update(key: ApiKey) = Result.success(dao.update(key))
    override suspend fun delete(key: ApiKey) = Result.success(dao.delete(key))
    override suspend fun deleteById(id: String) = Result.success(dao.deleteById(id))
    override fun getAllEnabled(): Flow<List<ApiKey>> = dao.getAllEnabled()
    override suspend fun getByUsageType(usageType: String) = Result.success(dao.getByUsageType(usageType))
    override suspend fun getById(id: String) = Result.success(dao.getById(id))
    override suspend fun incrementUsage(id: String, tokens: Long) = Result.success(dao.incrementUsage(id, tokens))
    override suspend fun recordError(id: String, time: Long) = Result.success(dao.recordError(id, time))
    override suspend fun recordTestSuccess(id: String, time: Long) = Result.success(dao.recordTestSuccess(id, time))
}
