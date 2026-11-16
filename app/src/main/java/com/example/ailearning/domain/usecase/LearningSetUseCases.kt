package com.example.ailearning.domain.usecase

import com.example.ailearning.domain.model.LearningSet
import com.example.ailearning.domain.repository.LearningSetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CreateLearningSetUseCase @Inject constructor(
    private val repository: LearningSetRepository
) {
    operator suspend fun invoke(
        name: String,
        subject: String,
        category: String,
        unit: String,
        scope: String
    ): Result<LearningSet> {
        return try {
            val set = LearningSet.create(name, subject, category, unit, scope)
            val id = repository.insert(set).getOrThrow()
            Result.success(set.copy(id = id.toString()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetLearningSetsUseCase @Inject constructor(
    private val repository: LearningSetRepository
) {
    operator fun invoke(): Flow<List<LearningSet>> = repository.getAllActive()
}

class GetLearningSetUseCase @Inject constructor(
    private val repository: LearningSetRepository
) {
    operator fun invoke(id: String): Flow<LearningSet?> = repository.getById(id)
}

class UpdateLearningSetUseCase @Inject constructor(
    private val repository: LearningSetRepository
) {
    operator suspend fun invoke(set: LearningSet): Result<Unit> {
        return try {
            repository.update(set.copy(updatedAt = System.currentTimeMillis())).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteLearningSetUseCase @Inject constructor(
    private val repository: LearningSetRepository
) {
    operator suspend fun invoke(id: String): Result<Unit> {
        return try {
            repository.deleteById(id).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
