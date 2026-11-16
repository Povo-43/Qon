package com.example.ailearning.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore

@Entity(tableName = "difficulty_stats")
data class DifficultyStat(
    @ColumnInfo(name = "difficulty") val difficulty: Int = 0,
    @ColumnInfo(name = "total") val total: Int = 0,
    @ColumnInfo(name = "correct") val correct: Int = 0,
    @ColumnInfo(name = "avgTime") val avgTime: Double = 0.0
) {
    @Ignore
    val accuracy: Double = if (total > 0) correct.toDouble() / total else 0.0
}
