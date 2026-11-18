package com.example.ailearning.domain.repository

import com.example.ailearning.domain.model.FocusBlockedApp
import kotlinx.coroutines.flow.Flow

interface FocusRepository {
    fun getBlockedApps(): Flow<List<FocusBlockedApp>>
    suspend fun getBlockedAppsOnce(): List<FocusBlockedApp>
    suspend fun getEnabledBlockedAppsOnce(): List<FocusBlockedApp>
    suspend fun addBlockedApp(app: FocusBlockedApp): Result<Unit>
    suspend fun removeBlockedApp(packageName: String): Result<Unit>
    suspend fun setBlockedAppEnabled(packageName: String, enabled: Boolean): Result<Unit>
    suspend fun isBlocked(packageName: String): Result<Boolean>
}
