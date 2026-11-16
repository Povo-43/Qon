package com.example.ailearning.di

import com.example.ailearning.data.local.encryption.EncryptionManager
import com.example.ailearning.data.remote.api.AiApiClient
import com.example.ailearning.data.remote.api.AiClientFactory
import com.example.ailearning.data.remote.api.AnthropicApiClient
import com.example.ailearning.data.remote.api.GeminiApiClient
import com.example.ailearning.data.remote.api.OpenAiApiClient
import com.example.ailearning.data.repository.AiRepositoryImpl
import com.example.ailearning.domain.repository.AiRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

object NetworkModule {
    
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    fun provideOpenAiClient(
        ioDispatcher: CoroutineDispatcher,
        encryptionManager: EncryptionManager
    ): OpenAiApiClient {
        // Default empty key - will be replaced when actually used
        return OpenAiApiClient(ioDispatcher, "", "gpt-4o-mini")
    }
    
    fun provideAnthropicClient(
        ioDispatcher: CoroutineDispatcher,
        encryptionManager: EncryptionManager
    ): AnthropicApiClient {
        return AnthropicApiClient(ioDispatcher, "", "claude-3-haiku-20240307")
    }
    
    fun provideGeminiClient(
        ioDispatcher: CoroutineDispatcher,
        encryptionManager: EncryptionManager
    ): GeminiApiClient {
        return GeminiApiClient(ioDispatcher, "", "gemini-1.5-flash")
    }
    
    fun provideAiClientFactory(
        ioDispatcher: CoroutineDispatcher,
        encryptionManager: EncryptionManager,
        openAiClient: OpenAiApiClient,
        anthropicClient: AnthropicApiClient,
        geminiClient: GeminiApiClient
    ): AiClientFactory {
        return AiClientFactory(ioDispatcher, encryptionManager, openAiClient, anthropicClient, geminiClient)
    }
    
    fun provideAiRepository(
        ioDispatcher: CoroutineDispatcher,
        apiKeyRepository: com.example.ailearning.domain.repository.ApiKeyRepository,
        clientFactory: AiClientFactory
    ): AiRepository {
        return AiRepositoryImpl(ioDispatcher, apiKeyRepository, clientFactory)
    }
}
