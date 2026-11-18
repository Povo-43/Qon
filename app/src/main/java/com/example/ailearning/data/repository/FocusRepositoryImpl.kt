package com.example.ailearning.data.repository

import com.example.ailearning.data.local.db.FocusBlockedAppDao
import com.example.ailearning.domain.model.FocusBlockedApp
import com.example.ailearning.domain.repository.FocusRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FocusRepositoryImpl @Inject constructor(
    private val dao: FocusBlockedAppDao
) : FocusRepository {
    override fun getBlockedApps(): Flow<List<FocusBlockedApp>> = dao.getAll()
    override suspend fun getBlockedAppsOnce(): List<FocusBlockedApp> = dao.getAllOnce()
    override suspend fun getEnabledBlockedAppsOnce(): List<FocusBlockedApp> = dao.getEnabledOnce()
    override suspend fun addBlockedApp(app: FocusBlockedApp): Result<Unit> =
        runCatching { dao.insert(app); Unit }

    override suspend fun removeBlockedApp(packageName: String): Result<Unit> =
        runCatching { dao.deleteByPackageName(packageName); Unit }

    override suspend fun setBlockedAppEnabled(packageName: String, enabled: Boolean): Result<Unit> =
        runCatching { dao.setEnabled(packageName, enabled) }

    override suspend fun isBlocked(packageName: String): Result<Boolean> =
        runCatching { dao.getByPackageName(packageName)?.enabled == true }
}
