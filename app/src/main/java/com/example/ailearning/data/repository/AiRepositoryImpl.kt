package com.example.ailearning.data.repository

import com.example.ailearning.data.remote.api.AiApiClient
import com.example.ailearning.data.remote.api.AiClientFactory
import com.example.ailearning.domain.model.AnalyzeLearningRequest
import com.example.ailearning.domain.model.AnalyzeLearningResponse
import com.example.ailearning.domain.model.ApiKey
import com.example.ailearning.domain.model.GenerateQuestionsRequest
import com.example.ailearning.domain.model.GenerateQuestionsResponse
import com.example.ailearning.domain.repository.AiRepository
import com.example.ailearning.domain.repository.ApiKeyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class AiRepositoryImpl @Inject constructor(
    @Named("io_dispatcher") private val ioDispatcher: CoroutineDispatcher,
    private val apiKeyRepository: ApiKeyRepository,
    private val clientFactory: AiClientFactory
) : AiRepository {
    
    override suspend fun generateQuestions(request: GenerateQuestionsRequest): Result<GenerateQuestionsResponse> = withContext(ioDispatcher) {
        val keysResult = apiKeyRepository.getByUsageType(ApiKey.USAGE_GENERATION)
        if (keysResult.isFailure) return@withContext Result.failure(keysResult.exceptionOrNull() ?: Exception("No API keys"))
        
        val keys = keysResult.getOrNull() ?: emptyList()
        if (keys.isEmpty()) return@withContext Result.failure(Exception("No API keys configured for question generation"))
        
        var lastException: Exception? = null
        
        for (key in keys) {
            val client = clientFactory.createClient(key)
            val result = client.generateQuestions(request)
            
            if (result.isSuccess) {
                apiKeyRepository.incrementUsage(key.id, result.getOrNull()?.tokensUsed?.toLong() ?: 0)
                return@withContext result
            } else {
                lastException = result.exceptionOrNull() as? Exception
                apiKeyRepository.recordError(key.id, System.currentTimeMillis())
            }
        }
        
        Result.failure(lastException ?: Exception("All API keys failed"))
    }
    
    override suspend fun analyzeLearning(request: AnalyzeLearningRequest): Result<AnalyzeLearningResponse> = withContext(ioDispatcher) {
        val keysResult = apiKeyRepository.getByUsageType(ApiKey.USAGE_ANALYSIS)
        if (keysResult.isFailure) return@withContext Result.failure(keysResult.exceptionOrNull() ?: Exception("No API keys"))
        
        val keys = keysResult.getOrNull() ?: emptyList()
        if (keys.isEmpty()) return@withContext Result.failure(Exception("No API keys configured for analysis"))
        
        var lastException: Exception? = null
        
        for (key in keys) {
            val client = clientFactory.createClient(key)
            val result = client.analyzeLearning(request)
            
            if (result.isSuccess) {
                apiKeyRepository.incrementUsage(key.id, result.getOrNull()?.tokensUsed?.toLong() ?: 0)
                return@withContext result
            } else {
                lastException = result.exceptionOrNull() as? Exception
                apiKeyRepository.recordError(key.id, System.currentTimeMillis())
            }
        }
        
        Result.failure(lastException ?: Exception("All API keys failed"))
    }
    
    override suspend fun testConnection(provider: String, apiKey: String, model: String, baseUrl: String?): Result<Unit> = withContext(ioDispatcher) {
        val client = clientFactory.createClientForProvider(provider, apiKey, model, baseUrl)
        client.testConnection()
    }
}
