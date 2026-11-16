package com.example.ailearning.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(
    tableName = "answer_histories",
    primaryKeys = ["id"],
    indices = [
        Index("questionId"),
        Index("learningSetId"),
        Index("answeredAt"),
        Index("sessionId")
    ]
)
data class AnswerHistory(
    val id: String = UUID.randomUUID().toString(),
    val questionId: String,
    val learningSetId: String,
    val selectedAnswer: Int,
    val isCorrect: Boolean,
    val responseTimeMs: Long,
    val answeredAt: Long = System.currentTimeMillis(),
    val sessionId: String,
    val source: String
) {
    companion object {
        const val SOURCE_APP = "app"
        const val SOURCE_WIDGET = "widget"
        const val SKIPPED = -1
    }
}
