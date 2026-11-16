package com.example.ailearning.data.repository

import com.example.ailearning.data.local.db.WidgetConfigDao
import com.example.ailearning.domain.model.WidgetConfig
import com.example.ailearning.domain.repository.WidgetConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WidgetConfigRepositoryImpl @Inject constructor(
    private val dao: WidgetConfigDao
) : WidgetConfigRepository {
    override suspend fun insert(config: WidgetConfig) = Result.success(dao.insert(config))
    override suspend fun update(config: WidgetConfig) = Result.success(dao.update(config))
    override suspend fun delete(config: WidgetConfig) = Result.success(dao.delete(config))
    override suspend fun getByWidgetId(widgetId: Int) = Result.success(dao.getByWidgetId(widgetId))
    override suspend fun getByLearningSetId(learningSetId: String) = Result.success(dao.getByLearningSetId(learningSetId))
    override fun getAll(): Flow<List<WidgetConfig>> = dao.getAll()
}
