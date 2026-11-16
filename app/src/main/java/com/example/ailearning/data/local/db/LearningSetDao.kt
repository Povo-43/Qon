package com.example.ailearning.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ailearning.domain.model.LearningSet
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningSetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: LearningSet): Long

    @Update
    suspend fun update(set: LearningSet): Int

    @Delete
    suspend fun delete(set: LearningSet): Int

    @Query("DELETE FROM learning_sets WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM learning_sets WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getAllActive(): Flow<List<LearningSet>>

    @Query("SELECT * FROM learning_sets WHERE id = :id")
    fun getById(id: String): Flow<LearningSet?>

    @Query("SELECT * FROM learning_sets WHERE id = :id")
    suspend fun getByIdOnce(id: String): LearningSet?
}
