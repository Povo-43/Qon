package com.example.ailearning.domain.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "widget_configs")
data class WidgetConfig(
    @PrimaryKey val widgetId: Int, // AppWidgetManagerのID
    val learningSetId: String,
    val updateIntervalMinutes: Int = 30,
    val questionCount: Int = 10,   // キャッシュする問題数
    val showExplanation: Boolean = true,
    val autoNext: Boolean = false,
    val lastUpdatedAt: Long = 0,
    val cachedQuestionIds: String = "" // JSON配列
) {
    @Ignore
    private val gson = Gson()

    fun getCachedQuestionIdList(): List<String> {
        return try {
            gson.fromJson<List<String>>(cachedQuestionIds, object : TypeToken<List<String>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setCachedQuestionIds(ids: List<String>): WidgetConfig {
        return copy(cachedQuestionIds = gson.toJson(ids))
    }
}
