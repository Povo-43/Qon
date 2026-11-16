package com.example.ailearning.data.remote.api

import com.example.ailearning.data.local.encryption.EncryptionManager
import com.example.ailearning.domain.model.ApiKey
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named

class AiClientFactory @Inject constructor(
    @Named("io_dispatcher") private val ioDispatcher: CoroutineDispatcher,
    private val encryptionManager: EncryptionManager,
    private val openAiClient: OpenAiApiClient,
    private val anthropicClient: AnthropicApiClient,
    private val geminiClient: GeminiApiClient
) {
    fun createClient(apiKey: ApiKey): AiApiClient {
        val decryptedKey = encryptionManager.getApiKey(apiKey.id) ?: ""
        
        return when (apiKey.provider) {
            ApiKey.PROVIDER_OPENAI -> OpenAiApiClient(ioDispatcher, decryptedKey, apiKey.model, apiKey.baseUrl ?: "https://api.openai.com/v1/")
            ApiKey.PROVIDER_ANTHROPIC -> AnthropicApiClient(ioDispatcher, decryptedKey, apiKey.model, apiKey.baseUrl ?: "https://api.anthropic.com/v1/")
            ApiKey.PROVIDER_GOOGLE -> GeminiApiClient(ioDispatcher, decryptedKey, apiKey.model, apiKey.baseUrl ?: "https://generativelanguage.googleapis.com/v1beta/")
            ApiKey.PROVIDER_CUSTOM -> OpenAiApiClient(ioDispatcher, decryptedKey, apiKey.model, apiKey.baseUrl ?: "https://api.openai.com/v1/")
            else -> OpenAiApiClient(ioDispatcher, decryptedKey, apiKey.model, apiKey.baseUrl ?: "https://api.openai.com/v1/")
        }
    }
    
    fun createClientForProvider(provider: String, apiKey: String, model: String, baseUrl: String?): AiApiClient {
        return when (provider) {
            ApiKey.PROVIDER_OPENAI -> OpenAiApiClient(ioDispatcher, apiKey, model, baseUrl ?: "https://api.openai.com/v1/")
            ApiKey.PROVIDER_ANTHROPIC -> AnthropicApiClient(ioDispatcher, apiKey, model, baseUrl ?: "https://api.anthropic.com/v1/")
            ApiKey.PROVIDER_GOOGLE -> GeminiApiClient(ioDispatcher, apiKey, model, baseUrl ?: "https://generativelanguage.googleapis.com/v1beta/")
            ApiKey.PROVIDER_CUSTOM -> OpenAiApiClient(ioDispatcher, apiKey, model, baseUrl ?: "https://api.openai.com/v1/")
            else -> OpenAiApiClient(ioDispatcher, apiKey, model, baseUrl ?: "https://api.openai.com/v1/")
        }
    }
}
