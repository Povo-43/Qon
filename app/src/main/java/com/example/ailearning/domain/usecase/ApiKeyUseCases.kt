package com.example.ailearning.domain.usecase

import com.example.ailearning.data.local.encryption.EncryptionManager
import com.example.ailearning.data.remote.api.AiClientFactory
import com.example.ailearning.domain.model.ApiKey
import com.example.ailearning.domain.repository.ApiKeyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class AddApiKeyUseCase @Inject constructor(
    private val repository: ApiKeyRepository,
    private val encryptionManager: EncryptionManager
) {
    operator suspend fun invoke(
        name: String,
        provider: String,
        model: String,
        rawKey: String,
        baseUrl: String?,
        priority: Int,
        usageType: String
    ): Result<ApiKey> {
        return try {
            encryptionManager.storeApiKey(name, rawKey)
            
            val key = ApiKey(
                name = name,
                provider = provider,
                model = model,
                encryptedKey = name, // キーIDとしてnameを使用（実際の暗号化キーはEncryptedSharedPreferencesに保存）
                baseUrl = baseUrl,
                priority = priority,
                usageType = usageType
            )
            
            val id = repository.insert(key).getOrThrow()
            Result.success(key.copy(id = id.toString()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetAvailableApiKeyUseCase @Inject constructor(
    private val repository: ApiKeyRepository
) {
    operator suspend fun invoke(usageType: String): Result<ApiKey> {
        val result = repository.getByUsageType(usageType)
        if (result.isFailure) return Result.failure(result.exceptionOrNull() ?: Exception("Failed to get API keys"))
        
        val keys = result.getOrNull() ?: emptyList()
        if (keys.isEmpty()) return Result.failure(Exception("No available API keys for $usageType"))
        
        return Result.success(keys.first())
    }
}

class TestApiKeyUseCase @Inject constructor(
    @Named("io_dispatcher") private val ioDispatcher: CoroutineDispatcher,
    private val repository: ApiKeyRepository,
    private val clientFactory: AiClientFactory
) {
    operator suspend fun invoke(keyId: String): Result<Boolean> = withContext(ioDispatcher) {
        val keyResult = repository.getById(keyId)
        if (keyResult.isFailure) return@withContext Result.failure(keyResult.exceptionOrNull() ?: Exception("Key not found"))
        
        val key = keyResult.getOrNull() ?: return@withContext Result.failure(Exception("Key not found"))
        val client = clientFactory.createClient(key)
        
        val result = client.testConnection()
        if (result.isSuccess) {
            repository.recordTestSuccess(keyId, System.currentTimeMillis())
            Result.success(true)
        } else {
            repository.recordError(keyId, System.currentTimeMillis())
            Result.failure(result.exceptionOrNull() ?: Exception("Connection test failed"))
        }
    }
}

class UpdateApiKeyUseCase @Inject constructor(
    private val repository: ApiKeyRepository,
    private val encryptionManager: EncryptionManager
) {
    operator suspend fun invoke(key: ApiKey): Result<Unit> {
        return try {
            repository.update(key).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteApiKeyUseCase @Inject constructor(
    private val repository: ApiKeyRepository,
    private val encryptionManager: EncryptionManager
) {
    operator suspend fun invoke(keyId: String): Result<Unit> {
        return try {
            val keyResult = repository.getById(keyId)
            if (keyResult.isSuccess) {
                val key = keyResult.getOrNull()
                if (key != null) {
                    encryptionManager.removeApiKey(key.id)
                }
            }
            repository.deleteById(keyId).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
