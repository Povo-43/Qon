package com.example.ailearning.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ailearning.domain.model.LearningSet
import com.example.ailearning.domain.usecase.CreateLearningSetUseCase
import com.example.ailearning.domain.usecase.DeleteLearningSetUseCase
import com.example.ailearning.domain.usecase.GetLearningSetsUseCase
import com.example.ailearning.domain.usecase.UpdateLearningSetUseCase
import com.example.ailearning.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LearningSetViewModel : ViewModel() {
    private val getLearningSetsUseCase = AppContainer.getLearningSetsUseCase
    private val createLearningSetUseCase = AppContainer.createLearningSetUseCase
    private val updateLearningSetUseCase = AppContainer.updateLearningSetUseCase
    private val deleteLearningSetUseCase = AppContainer.deleteLearningSetUseCase

    private val _learningSets = MutableStateFlow<List<LearningSet>>(emptyList())
    val learningSets: StateFlow<List<LearningSet>> = _learningSets
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadLearningSets()
    }
    
    fun loadLearningSets() {
        viewModelScope.launch {
            _isLoading.value = true
            getLearningSetsUseCase().collect { sets ->
                _learningSets.value = sets
                _isLoading.value = false
            }
        }
    }
    
    fun createLearningSet(
        name: String,
        subject: String,
        category: String,
        unit: String,
        scope: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = createLearningSetUseCase(name, subject, category, unit, scope)
            if (result.isSuccess) {
                loadLearningSets()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "作成に失敗しました"
            }
            _isLoading.value = false
        }
    }
    
    fun updateLearningSet(set: LearningSet) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = updateLearningSetUseCase(set)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "更新に失敗しました"
            }
            _isLoading.value = false
        }
    }
    
    fun deleteLearningSet(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = deleteLearningSetUseCase(id)
            if (result.isSuccess) {
                loadLearningSets()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "削除に失敗しました"
            }
            _isLoading.value = false
        }
    }
}
