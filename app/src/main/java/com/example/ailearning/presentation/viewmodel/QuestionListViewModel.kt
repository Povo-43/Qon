package com.example.ailearning.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ailearning.di.AppContainer
import com.example.ailearning.domain.model.QuestionWithStats
import com.example.ailearning.presentation.work.GenerationState
import com.example.ailearning.presentation.work.QuestionPrefetchScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 学習セットの問題一覧ページ用 ViewModel。
 *
 * - 問題と回答統計（正答率）を Room の Flow で監視し、生成・回答のたびに自動更新する
 * - 指定した問題数のバックグラウンド生成を WorkManager に依頼する
 */
class QuestionListViewModel : ViewModel() {

    private val questionRepository = AppContainer.questionRepository

    private val _items = MutableStateFlow<List<QuestionWithStats>>(emptyList())
    val items: StateFlow<List<QuestionWithStats>> = _items

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var loadedSetId: String? = null

    /** 問題一覧の購読を開始する。DBが更新されると自動で反映される。 */
    fun load(learningSetId: String) {
        if (loadedSetId == learningSetId) return
        loadedSetId = learningSetId
        viewModelScope.launch {
            _isLoading.value = true
            questionRepository.observeQuestionsWithStats(learningSetId).collect { list ->
                _items.value = list
                _isLoading.value = false
            }
        }
    }

    /** 指定した問題数をバックグラウンドで生成する（WorkManager・ネットワーク接続時のみ）。 */
    fun requestGeneration(learningSetId: String, count: Int) {
        QuestionPrefetchScheduler.requestGeneration(AppContainer.appContext, learningSetId, count)
    }

    /** バックグラウンド生成の状態（実行中/失敗）を監視する。 */
    fun observeGenerationState(learningSetId: String): Flow<GenerationState> =
        QuestionPrefetchScheduler.observeGenerationState(AppContainer.appContext, learningSetId)
}
