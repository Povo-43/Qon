package com.example.ailearning.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore

@Entity(tableName = "category_stats")
data class CategoryStat(
    @ColumnInfo(name = "category") val category: String = "",
    @ColumnInfo(name = "total") val total: Int = 0,
    @ColumnInfo(name = "correct") val correct: Int = 0,
    @ColumnInfo(name = "avgTime") val avgTime: Double = 0.0
) {
    @Ignore
    val accuracy: Double = if (total > 0) correct.toDouble() / total else 0.0
}
