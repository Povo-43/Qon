package com.example.ailearning.domain.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "questions",
    indices = [
        Index("learningSetId"),
        Index("category"),
        Index("difficulty"),
        Index("contentHash")
    ]
)
data class Question(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val learningSetId: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: Int,        // 0-3 (A-D)
    val explanation: String,
    val difficulty: Int,           // 1-5
    val category: String,          // 分野タグ
    val source: String,            // "ai_generated" / "manual"
    val aiModelUsed: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    @Embedded val duplicateCheck: DuplicateCheck = DuplicateCheck()
) {
    fun getOptions(): List<String> = listOf(optionA, optionB, optionC, optionD)
    
    fun getContentHash(): String = duplicateCheck.contentHash
    
    fun getOptionsHash(): String = duplicateCheck.optionsHash
    
    fun isCorrectAnswer(selected: Int): Boolean = selected == correctAnswer
    
    companion object {
        const val SOURCE_AI = "ai_generated"
        const val SOURCE_MANUAL = "manual"
        const val DIFFICULTY_MIN = 1
        const val DIFFICULTY_MAX = 5
    }
}

data class DuplicateCheck(
    val contentHash: String = "",
    val optionsHash: String = "",
    val similarityScore: Float = 0f
)
