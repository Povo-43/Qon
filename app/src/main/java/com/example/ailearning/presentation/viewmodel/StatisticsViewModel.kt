package com.example.ailearning.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ailearning.domain.model.AiAnalysis
import com.example.ailearning.domain.model.CategoryStatistics
import com.example.ailearning.domain.model.DailyStatistics
import com.example.ailearning.domain.model.DifficultyStatistics
import com.example.ailearning.domain.model.OverallStatistics
import com.example.ailearning.domain.model.StatisticsPeriod
import com.example.ailearning.domain.usecase.AnalyzeLearningUseCase
import com.example.ailearning.domain.usecase.GetCategoryStatisticsUseCase
import com.example.ailearning.domain.usecase.GetDifficultyStatisticsUseCase
import com.example.ailearning.domain.usecase.GetLatestAnalysisUseCase
import com.example.ailearning.domain.usecase.GetLearningTrendUseCase
import com.example.ailearning.domain.usecase.GetOverallStatisticsUseCase
import com.example.ailearning.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StatisticsViewModel : ViewModel() {
    private val getOverallStatisticsUseCase = AppContainer.getOverallStatisticsUseCase
    private val getCategoryStatisticsUseCase = AppContainer.getCategoryStatisticsUseCase
    private val getDifficultyStatisticsUseCase = AppContainer.getDifficultyStatisticsUseCase
    private val getLearningTrendUseCase = AppContainer.getLearningTrendUseCase
    private val analyzeLearningUseCase = AppContainer.analyzeLearningUseCase
    private val getLatestAnalysisUseCase = AppContainer.getLatestAnalysisUseCase
    
    private val _overallStats = MutableStateFlow<OverallStatistics?>(null)
    val overallStats: StateFlow<OverallStatistics?> = _overallStats
    
    private val _categoryStats = MutableStateFlow<List<CategoryStatistics>>(emptyList())
    val categoryStats: StateFlow<List<CategoryStatistics>> = _categoryStats
    
    private val _difficultyStats = MutableStateFlow<List<DifficultyStatistics>>(emptyList())
    val difficultyStats: StateFlow<List<DifficultyStatistics>> = _difficultyStats
    
    private val _trendData = MutableStateFlow<List<DailyStatistics>>(emptyList())
    val trendData: StateFlow<List<DailyStatistics>> = _trendData
    
    private val _aiAnalysis = MutableStateFlow<AiAnalysis?>(null)
    val aiAnalysis: StateFlow<AiAnalysis?> = _aiAnalysis
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private var currentLearningSetId: String = ""
    private var currentPeriod: StatisticsPeriod = StatisticsPeriod.ALL
    
    fun loadStatistics(learningSetId: String, period: StatisticsPeriod = StatisticsPeriod.ALL) {
        currentLearningSetId = learningSetId
        currentPeriod = period
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val overall = getOverallStatisticsUseCase(learningSetId, period)
            if (overall.isSuccess) _overallStats.value = overall.getOrNull()
            
            val category = getCategoryStatisticsUseCase(learningSetId, period)
            if (category.isSuccess) _categoryStats.value = category.getOrNull() ?: emptyList()
            
            val difficulty = getDifficultyStatisticsUseCase(learningSetId, period)
            if (difficulty.isSuccess) _difficultyStats.value = difficulty.getOrNull() ?: emptyList()
            
            val trend = getLearningTrendUseCase(learningSetId, when (period) {
                StatisticsPeriod.TODAY -> 1
                StatisticsPeriod.WEEK -> 7
                StatisticsPeriod.MONTH -> 30
                StatisticsPeriod.ALL -> 365
            })
            if (trend.isSuccess) _trendData.value = trend.getOrNull() ?: emptyList()
            
            // 最新のAI分析を取得
            val analysis = getLatestAnalysisUseCase(learningSetId, "weakness")
            if (analysis.isSuccess) _aiAnalysis.value = analysis.getOrNull()
            
            _isLoading.value = false
        }
    }
    
    fun generateAnalysis(type: String = "weakness") {
        if (currentLearningSetId.isEmpty()) return
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            _error.value = null
            
            val result = analyzeLearningUseCase(currentLearningSetId, type)
            if (result.isSuccess) {
                _aiAnalysis.value = result.getOrNull()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "分析に失敗しました"
            }
            
            _isAnalyzing.value = false
        }
    }
    
    fun refresh() {
        loadStatistics(currentLearningSetId, currentPeriod)
    }
}
