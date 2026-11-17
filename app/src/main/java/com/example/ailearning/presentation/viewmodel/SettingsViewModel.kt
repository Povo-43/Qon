package com.example.ailearning.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ailearning.domain.model.ApiKey
import com.example.ailearning.domain.usecase.AddApiKeyUseCase
import com.example.ailearning.domain.usecase.DeleteApiKeyUseCase
import com.example.ailearning.domain.usecase.GetAllApiKeysUseCase
import com.example.ailearning.domain.usecase.TestApiKeyUseCase
import com.example.ailearning.domain.usecase.UpdateApiKeyUseCase
import com.example.ailearning.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val addApiKeyUseCase = AppContainer.addApiKeyUseCase
    private val updateApiKeyUseCase = AppContainer.updateApiKeyUseCase
    private val deleteApiKeyUseCase = AppContainer.deleteApiKeyUseCase
    private val testApiKeyUseCase = AppContainer.testApiKeyUseCase
    private val getAllApiKeysUseCase = AppContainer.getAllApiKeysUseCase

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
}
