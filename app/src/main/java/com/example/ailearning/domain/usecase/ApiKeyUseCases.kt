package com.example.ailearning.domain.usecase

import com.example.ailearning.data.local.encryption.EncryptionManager
import com.example.ailearning.data.remote.api.AiClientFactory
import com.example.ailearning.domain.model.AiProviderCatalog
import com.example.ailearning.domain.model.ApiKey
import com.example.ailearning.domain.repository.ApiKeyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
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
            val key = ApiKey(
                name = name,
                provider = provider,
                model = model,
                encryptedKey = "",
                baseUrl = baseUrl,
                priority = priority,
                usageType = usageType
            )
            // 生のAPIキーはAndroid Keystoreで保護された領域にID紐付けで保存する
            encryptionManager.storeApiKey(key.id, rawKey)
            val stored = key.copy(encryptedKey = key.id)
            repository.insert(stored).getOrThrow()
            Result.success(stored)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetAllApiKeysUseCase @Inject constructor(
    private val repository: ApiKeyRepository
) {
    operator fun invoke(): Flow<List<ApiKey>> = repository.getAll()
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

/**
 * 入力中のプロバイダ設定からモデル一覧を取得する。
 * 編集中で API キー入力が空の場合は、保存済みのキーを復号して使用する。
 */
class FetchAvailableModelsUseCase @Inject constructor(
    private val encryptionManager: EncryptionManager,
    private val clientFactory: AiClientFactory
) {
    operator suspend fun invoke(
        provider: String,
        rawKey: String,
        existingKeyId: String?,
        model: String,
        baseUrl: String?
    ): Result<List<String>> {
        val info = AiProviderCatalog.resolve(provider)
        val resolvedKey = rawKey.ifBlank {
            existingKeyId?.let { encryptionManager.getApiKey(it) }.orEmpty()
        }

        if (resolvedKey.isBlank() && !info.isLocal) {
            return Result.failure(Exception("先に API キーを入力してください"))
        }
        if (info.requiresBaseUrl && baseUrl.isNullOrBlank()) {
            return Result.failure(Exception("先にベースURLを入力してください"))
        }

        return clientFactory
            .createClientForProvider(provider, resolvedKey, model, baseUrl)
            .listModels()
    }
}

class UpdateApiKeyUseCase @Inject constructor(
    private val repository: ApiKeyRepository,
    private val encryptionManager: EncryptionManager
) {
    operator suspend fun invoke(key: ApiKey, rawKey: String? = null): Result<Unit> {
        return try {
            if (!rawKey.isNullOrBlank()) {
                encryptionManager.storeApiKey(key.id, rawKey)
            }
            repository.update(key.copy(encryptedKey = key.id)).getOrThrow()
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
