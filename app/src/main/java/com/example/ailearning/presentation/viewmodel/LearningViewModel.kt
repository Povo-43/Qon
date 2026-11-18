package com.example.ailearning.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ailearning.domain.model.AnswerHistory
import com.example.ailearning.domain.model.LearningSession
import com.example.ailearning.domain.model.Question
import com.example.ailearning.domain.usecase.GenerateQuestionsUseCase
import com.example.ailearning.domain.usecase.GetQuestionsByCategoryUseCase
import com.example.ailearning.domain.usecase.GetQuestionsByDifficultyUseCase
import com.example.ailearning.domain.usecase.GetRandomQuestionsUseCase
import com.example.ailearning.domain.usecase.GetWeakQuestionsUseCase
import com.example.ailearning.domain.usecase.RecordAnswerUseCase
import com.example.ailearning.domain.usecase.StartLearningSessionUseCase
import com.example.ailearning.domain.usecase.EndLearningSessionUseCase
import com.example.ailearning.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeoutOrNull

class LearningViewModel : ViewModel() {
    private val generateQuestionsUseCase = AppContainer.generateQuestionsUseCase
    private val getRandomQuestionsUseCase = AppContainer.getRandomQuestionsUseCase
    private val getWeakQuestionsUseCase = AppContainer.getWeakQuestionsUseCase
    private val getQuestionsByCategoryUseCase = AppContainer.getQuestionsByCategoryUseCase
    private val getQuestionsByDifficultyUseCase = AppContainer.getQuestionsByDifficultyUseCase
    private val startSessionUseCase = AppContainer.startLearningSessionUseCase
    private val recordAnswerUseCase = AppContainer.recordAnswerUseCase
    private val endSessionUseCase = AppContainer.endSessionUseCase
    
    // State
    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions
    
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex
    
    private val _currentQuestion = MutableStateFlow<Question?>(null)
    val currentQuestion: StateFlow<Question?> = _currentQuestion
    
    private val _session = MutableStateFlow<LearningSession?>(null)
    val session: StateFlow<LearningSession?> = _session
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private val _showResult = MutableStateFlow(false)
    val showResult: StateFlow<Boolean> = _showResult
    
    private val _lastAnswer = MutableStateFlow<AnswerHistory?>(null)
    val lastAnswer: StateFlow<AnswerHistory?> = _lastAnswer
    
    private val _sessionComplete = MutableStateFlow(false)
    val sessionComplete: StateFlow<Boolean> = _sessionComplete
    
    var currentMode: String = "continuous"
    var currentCategory: String? = null
    var currentDifficulty: Int? = null

    /** バックグラウンド生成完了時に、セッションが変わっていないか確認するためのトークン */
    private var sessionToken = 0

    private var learningJob: Job? = null
    private var generatingJob: Job? = null
    
    val currentQuestionNumber: Int
        get() = _currentQuestionIndex.value + 1
    
    val totalQuestions: Int
        get() = _questions.value.size
    
    val progress: Float
        get() = if (totalQuestions > 0) currentQuestionNumber.toFloat() / totalQuestions else 0f
    
    fun startLearning(learningSetId: String, mode: String, count: Int = 10) {
        currentMode = mode
        learningJob?.cancel()
        learningJob = viewModelScope.launch {
            try {
                startLearningInternal(learningSetId, mode, count)
            } catch (e: CancellationException) {
                _error.value = "読み込みをキャンセルしました"
                throw e
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun startLearningInternal(learningSetId: String, mode: String, count: Int) {
        sessionToken++
        val token = sessionToken
        _isLoading.value = true
        _error.value = null
        _sessionComplete.value = false

        val sessionResult = startSessionUseCase(learningSetId, mode)
        if (sessionResult.isFailure) {
            _error.value = sessionResult.exceptionOrNull()?.message ?: "セッション開始に失敗"
            return
        }

        _session.value = sessionResult.getOrNull()

        var deferGeneration = false
        val questionsResult = when (mode) {
            "continuous" -> {
                // 保存済みの問題があれば即座に開始し、新しい問題は裏で生成する
                val stored = getRandomQuestionsUseCase(learningSetId, count).getOrElse { emptyList() }
                if (stored.isNotEmpty()) {
                    deferGeneration = true
                    Result.success(stored)
                } else {
                    generateWithTimeout(learningSetId, count)
                }
            }
            "review" -> getWeakQuestionsUseCase(learningSetId, count)
            "random" -> getRandomQuestionsUseCase(learningSetId, count)
            "unit" -> {
                if (currentCategory != null) {
                    getQuestionsByCategoryUseCase(learningSetId, currentCategory!!, count)
                } else if (currentDifficulty != null) {
                    getQuestionsByDifficultyUseCase(learningSetId, currentDifficulty!!, count)
                } else {
                    getRandomQuestionsUseCase(learningSetId, count)
                }
            }
            else -> getRandomQuestionsUseCase(learningSetId, count)
        }

        if (questionsResult.isSuccess) {
            val qs = questionsResult.getOrNull() ?: emptyList()
            if (qs.isEmpty()) {
                _error.value = when (mode) {
                    "review" -> "復習する問題がありません。\nまずは連続学習で問題を生成しましょう。"
                    "random", "unit" -> "保存済みの問題がありません。\n連続学習を実行するか、問題を生成してください。"
                    else -> "問題を生成できませんでした。もう一度お試しください。"
                }
            }
            _questions.value = qs
            _currentQuestionIndex.value = 0
            _currentQuestion.value = qs.firstOrNull()

            if (deferGeneration) {
                viewModelScope.launch {
                    _isGenerating.value = true
                    try {
                        val result = generateWithTimeout(learningSetId, count)
                        // 生成中に別セッションへ切り替わっていたら反映しない
                        if (token == sessionToken && result.isSuccess) {
                            val fresh = result.getOrDefault(emptyList())
                            if (fresh.isNotEmpty()) {
                                _questions.value = _questions.value + fresh
                            }
                        }
                    } finally {
                        if (token == sessionToken) {
                            _isGenerating.value = false
                        }
                    }
                }
            }
        } else {
            _error.value = questionsResult.exceptionOrNull()?.message ?: "問題の取得に失敗"
        }
    }

    /** 生成を最大 {@link GENERATION_TIMEOUT_MS} まで待ち、超過したらタイムアウト扱いにする。 */
    private suspend fun generateWithTimeout(learningSetId: String, count: Int): Result<List<Question>> {
        val result = withTimeoutOrNull(GENERATION_TIMEOUT_MS) {
            generateQuestionsUseCase(learningSetId, count)
        }
        return result ?: Result.failure(
            Exception(
                "問題生成がタイムアウトしました（${GENERATION_TIMEOUT_MS / 1000}秒）。\n" +
                    "ネットワーク状況を確認するか、少し待ってから再試行してください。"
            )
        )
    }

    /** 読み込み・生成をキャンセルする（ローディングのキャンセルボタン用）。 */
    fun cancelGeneration() {
        learningJob?.cancel()
        generatingJob?.cancel()
        learningJob = null
        generatingJob = null
        _isLoading.value = false
        _isGenerating.value = false
    }

    fun generateMoreQuestions(learningSetId: String, count: Int = 10) {
        generatingJob?.cancel()
        generatingJob = viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            try {
                val result = generateWithTimeout(learningSetId, count)
                if (result.isSuccess) {
                    val newQuestions = result.getOrNull() ?: emptyList()
                    _questions.value = _questions.value + newQuestions
                    if (_currentQuestion.value == null) {
                        _currentQuestion.value = _questions.value.firstOrNull()
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "問題生成に失敗"
                }
            } catch (e: CancellationException) {
                // キャンセル時はエラーを出さない
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /** エラー表示を消す（再試行ボタン用）。 */
    fun clearError() {
        _error.value = null
    }
    
    fun submitAnswer(selectedAnswer: Int, responseTimeMs: Long, source: String = "app") {
        val question = _currentQuestion.value ?: return
        val session = _session.value ?: return
        
        viewModelScope.launch {
            val result = recordAnswerUseCase(
                sessionId = session.id,
                questionId = question.id,
                learningSetId = session.learningSetId,
                selectedAnswer = selectedAnswer,
                responseTimeMs = responseTimeMs,
                source = source
            )
            
            if (result.isSuccess) {
                val history = result.getOrNull()!!
                _lastAnswer.value = history
                _showResult.value = true
                // 画面上のセッション集計も即時更新する
                _session.value = _session.value?.let { current ->
                    current.copy(
                        totalQuestions = current.totalQuestions + 1,
                        correctAnswers = current.correctAnswers + if (history.isCorrect) 1 else 0,
                        totalTimeMs = current.totalTimeMs + responseTimeMs
                    )
                }
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "回答記録に失敗"
            }
        }
    }
    
    fun nextQuestion() {
        _showResult.value = false
        _lastAnswer.value = null
        
        val nextIndex = _currentQuestionIndex.value + 1
        if (nextIndex < _questions.value.size) {
            _currentQuestionIndex.value = nextIndex
            _currentQuestion.value = _questions.value[nextIndex]
        } else {
            // セッション終了
            completeSession()
        }
    }
    
    fun skipQuestion() {
        submitAnswer(AnswerHistory.SKIPPED, 0, "app")
    }
    
    private fun completeSession() {
        _session.value?.let { session ->
            viewModelScope.launch {
                endSessionUseCase(session.id)
                _sessionComplete.value = true
            }
        }
    }
    
    fun reset() {
        sessionToken++
        learningJob?.cancel()
        generatingJob?.cancel()
        learningJob = null
        generatingJob = null
        _questions.value = emptyList()
        _currentQuestionIndex.value = 0
        _currentQuestion.value = null
        _session.value = null
        _showResult.value = false
        _lastAnswer.value = null
        _sessionComplete.value = false
        _error.value = null
    }

    companion object {
        /** AI生成の待ち時間上限（ミリ秒）。 */
        const val GENERATION_TIMEOUT_MS = 150_000L
    }
}
