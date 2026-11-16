package com.example.ailearning.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ailearning.domain.model.ApiKey
import com.example.ailearning.domain.usecase.AddApiKeyUseCase
import com.example.ailearning.domain.usecase.DeleteApiKeyUseCase
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
    
    fun setApiKeys(keys: List<ApiKey>) {
        _apiKeys.value = keys
    }
    
    fun addApiKey(
        name: String,
        provider: String,
        model: String,
        apiKey: String,
        baseUrl: String?,
        priority: Int,
        usageType: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = addApiKeyUseCase(name, provider, model, apiKey, baseUrl, priority, usageType)
            if (result.isSuccess) {
                // 成功時は外部でリロード
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "追加に失敗しました"
            }
            _isLoading.value = false
        }
    }
    
    fun updateApiKey(key: ApiKey) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = updateApiKeyUseCase(key)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "更新に失敗しました"
            }
            _isLoading.value = false
        }
    }
    
    fun deleteApiKey(keyId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = deleteApiKeyUseCase(keyId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "削除に失敗しました"
            }
            _isLoading.value = false
        }
    }
    
    fun testApiKey(keyId: String) {
        viewModelScope.launch {
            _isTesting.value = _isTesting.value + keyId
            _testResult.value = _testResult.value + (keyId to false)
            
            val result = testApiKeyUseCase(keyId)
            val success = result.isSuccess
            
            _isTesting.value = _isTesting.value - keyId
            _testResult.value = _testResult.value + (keyId to success)
        }
    }
    
    fun toggleApiKeyEnabled(key: ApiKey) {
        val updated = key.copy(isEnabled = !key.isEnabled)
        updateApiKey(updated)
    }
    
    fun updatePriority(key: ApiKey, priority: Int) {
        val updated = key.copy(priority = priority)
        updateApiKey(updated)
    }
}
