package com.example.ailearning.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "ai_analyses")
data class AiAnalysis(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val learningSetId: String,
    val analysisType: String,      // "weakness" / "trend" / "recommendation"
    val content: String,           // JSON形式の分析結果
    val basedOnHistoryCount: Int,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_WEAKNESS = "weakness"
        const val TYPE_TREND = "trend"
        const val TYPE_RECOMMENDATION = "recommendation"
        val ALL_TYPES = listOf(TYPE_WEAKNESS, TYPE_TREND, TYPE_RECOMMENDATION)
    }
}
