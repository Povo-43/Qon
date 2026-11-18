package com.example.ailearning.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ailearning.domain.model.FocusBlockedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusBlockedAppDao {

    @Query("SELECT * FROM focus_blocked_apps ORDER BY addedAt ASC")
    fun getAll(): Flow<List<FocusBlockedApp>>

    @Query("SELECT * FROM focus_blocked_apps ORDER BY addedAt ASC")
    suspend fun getAllOnce(): List<FocusBlockedApp>

    @Query("SELECT * FROM focus_blocked_apps WHERE enabled = 1")
    suspend fun getEnabledOnce(): List<FocusBlockedApp>

    @Query("SELECT * FROM focus_blocked_apps WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): FocusBlockedApp?

    @Query("SELECT COUNT(*) FROM focus_blocked_apps WHERE enabled = 1")
    suspend fun countEnabled(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: FocusBlockedApp)

    @Query("DELETE FROM focus_blocked_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("UPDATE focus_blocked_apps SET enabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)
}
