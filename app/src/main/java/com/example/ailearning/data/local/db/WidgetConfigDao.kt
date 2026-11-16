package com.example.ailearning.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ailearning.domain.model.WidgetConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: WidgetConfig): Long

    @Update
    suspend fun update(config: WidgetConfig): Int

    @Delete
    suspend fun delete(config: WidgetConfig): Int

    @Query("SELECT * FROM widget_configs WHERE widgetId = :widgetId")
    suspend fun getByWidgetId(widgetId: Int): WidgetConfig?

    @Query("SELECT * FROM widget_configs WHERE learningSetId = :learningSetId")
    suspend fun getByLearningSetId(learningSetId: String): List<WidgetConfig>

    @Query("SELECT * FROM widget_configs")
    fun getAll(): Flow<List<WidgetConfig>>
}
