package com.example.ailearning.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ailearning.domain.model.ApiKey
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: ApiKey): Long

    @Update
    suspend fun update(key: ApiKey): Int

    @Delete
    suspend fun delete(key: ApiKey): Int

    @Query("DELETE FROM api_keys WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM api_keys WHERE isEnabled = 1 ORDER BY priority DESC, createdAt ASC")
    fun getAllEnabled(): Flow<List<ApiKey>>

    @Query("SELECT * FROM api_keys WHERE isEnabled = 1 AND usageType IN (:usageType, 'both') ORDER BY priority DESC")
    suspend fun getByUsageType(usageType: String): List<ApiKey>

    @Query("SELECT * FROM api_keys WHERE id = :id")
    suspend fun getById(id: String): ApiKey?

    @Query("UPDATE api_keys SET totalRequests = totalRequests + 1, totalTokensUsed = totalTokensUsed + :tokens WHERE id = :id")
    suspend fun incrementUsage(id: String, tokens: Long): Int

    @Query("UPDATE api_keys SET errorCount = errorCount + 1, lastTestedAt = :time, lastTestSuccess = 0 WHERE id = :id")
    suspend fun recordError(id: String, time: Long): Int

    @Query("UPDATE api_keys SET lastTestedAt = :time, lastTestSuccess = 1, errorCount = 0 WHERE id = :id")
    suspend fun recordTestSuccess(id: String, time: Long): Int
}
