package com.example.ailearning.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ailearning.domain.model.WidgetConfig
import com.example.ailearning.domain.usecase.CreateWidgetConfigUseCase
import com.example.ailearning.domain.usecase.DeleteWidgetConfigUseCase
import com.example.ailearning.domain.usecase.UpdateWidgetCacheUseCase
import com.example.ailearning.domain.usecase.UpdateWidgetConfigUseCase
import com.example.ailearning.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WidgetViewModel : ViewModel() {
    private val createWidgetConfigUseCase = AppContainer.createWidgetConfigUseCase
    private val updateWidgetConfigUseCase = AppContainer.updateWidgetConfigUseCase
    private val deleteWidgetConfigUseCase = AppContainer.deleteWidgetConfigUseCase
    private val updateWidgetCacheUseCase = AppContainer.updateWidgetCacheUseCase

    private val _configs = MutableStateFlow<List<WidgetConfig>>(emptyList())
    val configs: StateFlow<List<WidgetConfig>> = _configs
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    fun setConfigs(configs: List<WidgetConfig>) {
        _configs.value = configs
    }
    
    fun createWidgetConfig(widgetId: Int, learningSetId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = createWidgetConfigUseCase(widgetId, learningSetId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "作成に失敗しました"
            }
            _isLoading.value = false
        }
    }
    
    fun updateWidgetConfig(config: WidgetConfig) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = updateWidgetConfigUseCase(config)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "更新に失敗しました"
            }
            _isLoading.value = false
        }
    }
    
    fun deleteWidgetConfig(widgetId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = deleteWidgetConfigUseCase(widgetId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "削除に失敗しました"
            }
            _isLoading.value = false
        }
    }
    
    fun refreshWidgetCache(widgetId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = updateWidgetCacheUseCase(widgetId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "キャッシュ更新に失敗"
            }
            _isLoading.value = false
        }
    }
}
