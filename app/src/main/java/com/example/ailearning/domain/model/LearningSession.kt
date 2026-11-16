package com.example.ailearning.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "learning_sessions")
data class LearningSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val learningSetId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long = 0,
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val totalTimeMs: Long = 0,
    val mode: String               // "continuous" / "review" / "random" / "unit"
) {
    fun getAccuracy(): Double = if (totalQuestions > 0) correctAnswers.toDouble() / totalQuestions else 0.0
    
    fun getAvgResponseTimeMs(): Long = if (totalQuestions > 0) totalTimeMs / totalQuestions else 0
    
    companion object {
        const val MODE_CONTINUOUS = "continuous"
        const val MODE_REVIEW = "review"
        const val MODE_RANDOM = "random"
        const val MODE_UNIT = "unit"
        val ALL_MODES = listOf(MODE_CONTINUOUS, MODE_REVIEW, MODE_RANDOM, MODE_UNIT)
    }
}
