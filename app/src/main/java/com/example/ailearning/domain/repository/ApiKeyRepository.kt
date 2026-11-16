package com.example.ailearning.domain.repository

import com.example.ailearning.domain.model.ApiKey
import kotlinx.coroutines.flow.Flow

interface ApiKeyRepository {
    suspend fun insert(key: ApiKey): Result<Long>
    suspend fun update(key: ApiKey): Result<Int>
    suspend fun delete(key: ApiKey): Result<Int>
    suspend fun deleteById(id: String): Result<Int>
    fun getAllEnabled(): Flow<List<ApiKey>>
    suspend fun getByUsageType(usageType: String): Result<List<ApiKey>>
    suspend fun getById(id: String): Result<ApiKey?>
    suspend fun incrementUsage(id: String, tokens: Long): Result<Int>
    suspend fun recordError(id: String, time: Long): Result<Int>
    suspend fun recordTestSuccess(id: String, time: Long): Result<Int>
}
