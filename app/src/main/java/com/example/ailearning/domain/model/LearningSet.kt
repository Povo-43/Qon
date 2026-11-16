package com.example.ailearning.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "learning_sets")
data class LearningSet(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val subject: String,
    val category: String,
    val unit: String,
    val scope: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    companion object {
        fun create(
            name: String,
            subject: String,
            category: String,
            unit: String,
            scope: String
        ): LearningSet = LearningSet(
            name = name,
            subject = subject,
            category = category,
            unit = unit,
            scope = scope
        )
    }
}
