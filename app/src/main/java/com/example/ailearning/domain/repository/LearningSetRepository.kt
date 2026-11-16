package com.example.ailearning.domain.repository

import com.example.ailearning.domain.model.LearningSet
import kotlinx.coroutines.flow.Flow

interface LearningSetRepository {
    suspend fun insert(set: LearningSet): Result<Long>
    suspend fun update(set: LearningSet): Result<Int>
    suspend fun delete(set: LearningSet): Result<Int>
    suspend fun deleteById(id: String): Result<Int>
    fun getAllActive(): Flow<List<LearningSet>>
    fun getById(id: String): Flow<LearningSet?>
    suspend fun getByIdOnce(id: String): Result<LearningSet?>
}
