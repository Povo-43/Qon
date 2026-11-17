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
    val cachedQuestionIds: String = "", // JSON配列
    // --- カスタマイズ項目 (v1.5.0) ---
    /** プリセットテーマ ID（背景・アクセントを個別指定していない場合の既定） */
    val themeId: String = "indigo",
    /** 背景色（ARGB）。0 なら themeId の既定色 */
    val backgroundColor: Int = 0,
    /** アクセント色（ARGB）。0 なら themeId の既定色 */
    val accentColor: Int = 0,
    /** キャッシュ問題のうち現在表示中のインデックス */
    val currentIndex: Int = 0
) {
    @Ignore
    private val gson = Gson()

    fun getCachedQuestionIdList(): List<String> {
        if (cachedQuestionIds.isBlank()) return emptyList()
        return try {
            gson.fromJson<List<String>>(cachedQuestionIds, object : TypeToken<List<String>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setCachedQuestionIds(ids: List<String>): WidgetConfig {
        return copy(cachedQuestionIds = gson.toJson(ids))
    }

    /** 表示中の色設定（プリセット + 個別上書き）を解決する。 */
    fun resolvedColors(): WidgetColors {
        val preset = WidgetColors.preset(themeId)
        return preset.withOverrides(backgroundColor, accentColor)
    }
}
