package com.example.ailearning.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ailearning.domain.model.AiProviderCatalog
import com.example.ailearning.domain.model.ApiKey
import com.example.ailearning.domain.usecase.AddApiKeyUseCase
import com.example.ailearning.domain.usecase.DeleteApiKeyUseCase
import com.example.ailearning.domain.usecase.FetchAvailableModelsUseCase
import com.example.ailearning.domain.usecase.GetAllApiKeysUseCase
import com.example.ailearning.domain.usecase.TestApiKeyUseCase
import com.example.ailearning.domain.usecase.UpdateApiKeyUseCase
import com.example.ailearning.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val addApiKeyUseCase = AppContainer.addApiKeyUseCase
    private val updateApiKeyUseCase = AppContainer.updateApiKeyUseCase
    private val deleteApiKeyUseCase = AppContainer.deleteApiKeyUseCase
    private val testApiKeyUseCase = AppContainer.testApiKeyUseCase
    private val getAllApiKeysUseCase = AppContainer.getAllApiKeysUseCase
    private val fetchAvailableModelsUseCase = AppContainer.fetchAvailableModelsUseCase

    private val _apiKeys = MutableStateFlow<List<ApiKey>>(emptyList())
    val apiKeys: StateFlow<List<ApiKey>> = _apiKeys

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isTesting = MutableStateFlow<Set<String>>(emptySet())
    val isTesting: StateFlow<Set<String>> = _isTesting

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _testResult = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val testResult: StateFlow<Map<String, Boolean>> = _testResult

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels

    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels: StateFlow<Boolean> = _isFetchingModels

    private val _modelFetchError = MutableStateFlow<String?>(null)
    val modelFetchError: StateFlow<String?> = _modelFetchError

    private var modelFetchJob: Job? = null

    init {
        loadApiKeys()
    }

    fun loadApiKeys() {
        viewModelScope.launch {
            getAllApiKeysUseCase().collect { keys ->
                _apiKeys.value = keys
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun addApiKey(
        name: String,
        provider: String,
        model: String,
        apiKey: String,
        baseUrl: String?,
        priority: Int,
        usageType: String
    ): Result<Unit> {
        _error.value = null
        val result = addApiKeyUseCase(name, provider, model, apiKey, baseUrl, priority, usageType)
        if (result.isFailure) {
            _error.value = result.exceptionOrNull()?.message ?: "APIキーの保存に失敗しました"
        }
        return result.map { Unit }
    }

    suspend fun updateApiKey(key: ApiKey, rawKey: String? = null): Result<Unit> {
        _error.value = null
        val result = updateApiKeyUseCase(key, rawKey)
        if (result.isFailure) {
            _error.value = result.exceptionOrNull()?.message ?: "更新に失敗しました"
        }
        return result
    }

    suspend fun deleteApiKey(keyId: String): Result<Unit> {
        _isLoading.value = true
        _error.value = null
        val result = deleteApiKeyUseCase(keyId)
        if (result.isFailure) {
            _error.value = result.exceptionOrNull()?.message ?: "削除に失敗しました"
        }
        _isLoading.value = false
        return result
    }

    fun testApiKey(keyId: String) {
        viewModelScope.launch {
            _isTesting.value = _isTesting.value + keyId
            _testResult.value = _testResult.value - keyId

            val result = testApiKeyUseCase(keyId)
            val success = result.isSuccess

            _isTesting.value = _isTesting.value - keyId
            _testResult.value = _testResult.value + (keyId to success)
        }
    }

    suspend fun toggleApiKeyEnabled(key: ApiKey) {
        updateApiKey(key.copy(isEnabled = !key.isEnabled))
    }

    /**
     * 入力中のプロバイダ設定でモデル一覧を取得する。
     * 編集中でキー入力が空の場合は保存済みキーを使用する。
     */
    fun fetchModels(
        provider: String,
        baseUrl: String,
        rawKey: String,
        existingKeyId: String?,
        model: String
    ) {
        modelFetchJob?.cancel()
        modelFetchJob = viewModelScope.launch {
            _isFetchingModels.value = true
            _modelFetchError.value = null
            _availableModels.value = emptyList()

            val result = fetchAvailableModelsUseCase(
                provider = provider,
                rawKey = rawKey,
                existingKeyId = existingKeyId,
                model = model,
                baseUrl = baseUrl.ifBlank { null }
            )

            _isFetchingModels.value = false
            result
                .onSuccess { models ->
                    if (models.isEmpty()) {
                        showModelFallback(provider, "利用可能なモデルが見つかりませんでした")
                    } else {
                        _availableModels.value = models
                    }
                }
                .onFailure { error ->
                    // /models を提供しないプロバイダでも選択できるよう、カタログの候補を表示する
                    showModelFallback(
                        provider,
                        error.message ?: "モデル一覧の取得に失敗しました"
                    )
                }
        }
    }

    private fun showModelFallback(provider: String, reason: String) {
        val candidates = AiProviderCatalog.resolve(provider).models
        if (candidates.isNotEmpty()) {
            _availableModels.value = candidates
            _modelFetchError.value = "自動取得に失敗したため候補を表示しています（$reason）"
        } else {
            _modelFetchError.value = reason
        }
    }

    fun clearModelFetchState() {
        modelFetchJob?.cancel()
        modelFetchJob = null
        _isFetchingModels.value = false
        _availableModels.value = emptyList()
        _modelFetchError.value = null
    }
}
