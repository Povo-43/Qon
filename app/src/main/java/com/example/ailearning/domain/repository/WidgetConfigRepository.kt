package com.example.ailearning.domain.repository

import com.example.ailearning.domain.model.WidgetConfig
import kotlinx.coroutines.flow.Flow

interface WidgetConfigRepository {
    suspend fun insert(config: WidgetConfig): Result<Long>
    suspend fun update(config: WidgetConfig): Result<Int>
    suspend fun delete(config: WidgetConfig): Result<Int>
    suspend fun getByWidgetId(widgetId: Int): Result<WidgetConfig?>
    suspend fun getByLearningSetId(learningSetId: String): Result<List<WidgetConfig>>
    fun getAll(): Flow<List<WidgetConfig>>
}
