package com.example.ailearning.domain.usecase

import com.example.ailearning.domain.model.Question
import com.example.ailearning.domain.model.WidgetConfig
import com.example.ailearning.domain.repository.AnswerHistoryRepository
import com.example.ailearning.domain.repository.QuestionRepository
import com.example.ailearning.domain.repository.WidgetConfigRepository
import com.google.gson.Gson
import javax.inject.Inject

/** 候補として引く最大件数（ローカルDBなので十分な数を確保する） */
private const val WIDGET_FETCH_LIMIT = 1000

class UpdateWidgetCacheUseCase @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val widgetConfigRepository: WidgetConfigRepository
) {
    operator suspend fun invoke(widgetId: Int): Result<Unit> {
        return try {
            val configResult = widgetConfigRepository.getByWidgetId(widgetId)
            if (configResult.isFailure) return Result.failure(configResult.exceptionOrNull() ?: Exception("Widget config not found"))

            val config = configResult.getOrNull() ?: return Result.failure(Exception("Widget config not found"))

            // 回答統計に基づく重み付き選択でキャッシュを更新する。
            // （未回答優先・正答率の低い問題を多めに。全問題を既に回答済みでも
            //   除外で空になることがなく、キャッシュが更新され続ける）
            val candidates = questionRepository.getQuestionsWithStats(config.learningSetId, WIDGET_FETCH_LIMIT)
                .getOrElse { emptyList() }
            val questions = QuestionSelectionPolicy
                .select(candidates, config.questionCount)

            if (questions.isNotEmpty()) {
                val gson = Gson()
                val updatedConfig = config.copy(
                    lastUpdatedAt = System.currentTimeMillis(),
                    cachedQuestionIds = gson.toJson(questions.map { it.id }),
                    currentIndex = 0
                )
                widgetConfigRepository.update(updatedConfig).getOrThrow()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetWidgetQuestionUseCase @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val widgetConfigRepository: WidgetConfigRepository
) {
    operator suspend fun invoke(widgetId: Int): Result<Question?> {
        return try {
            val configResult = widgetConfigRepository.getByWidgetId(widgetId)
            if (configResult.isFailure) return Result.failure(configResult.exceptionOrNull() ?: Exception("Widget config not found"))

            val config = configResult.getOrNull() ?: return Result.failure(Exception("Widget config not found"))

            val cachedIds = config.getCachedQuestionIdList()
            if (cachedIds.isEmpty()) {
                // 回答統計に基づく重み付き選択（未回答優先・毎回正解の問題は出にくい）
                val candidates = questionRepository.getQuestionsWithStats(config.learningSetId, WIDGET_FETCH_LIMIT)
                    .getOrElse { emptyList() }
                return Result.success(QuestionSelectionPolicy.selectOne(candidates))
            }

            // currentIndex が範囲外なら先頭に戻す
            val index = config.currentIndex
            val safeIndex = if (index in cachedIds.indices) index else 0
            val questionId = cachedIds.getOrNull(safeIndex) ?: cachedIds.first()
            return questionRepository.getById(questionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 「次へ」でキャッシュ内の次の問題へ進み、進んだ先の問題を返す。
 * キャッシュが空・未設定の場合はランダムに1問取得する。
 */
class AdvanceWidgetQuestionUseCase @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val widgetConfigRepository: WidgetConfigRepository
) {
    suspend fun next(widgetId: Int): Question? {
        val config = widgetConfigRepository.getByWidgetId(widgetId).getOrNull() ?: return null

        val cachedIds = config.getCachedQuestionIdList()
        val nextQuestion: Question? = if (cachedIds.isEmpty()) {
            // 回答統計に基づく重み付き選択（未回答優先・毎回正解の問題は出にくい）
            val candidates = questionRepository.getQuestionsWithStats(config.learningSetId, WIDGET_FETCH_LIMIT)
                .getOrElse { emptyList() }
            QuestionSelectionPolicy.selectOne(candidates)
        } else {
            val nextIndex = ((config.currentIndex + 1).mod(cachedIds.size))
            widgetConfigRepository.update(config.copy(currentIndex = nextIndex)).getOrThrow()
            questionRepository.getById(cachedIds[nextIndex]).getOrNull()
        }
        return nextQuestion
    }
}

class ProcessWidgetAnswerUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository,
    private val questionRepository: QuestionRepository,
    private val widgetConfigRepository: WidgetConfigRepository
) {
    operator suspend fun invoke(
        widgetId: Int,
        questionId: String,
        selectedAnswer: Int,
        responseTimeMs: Long
    ): Result<WidgetAnswerResult> {
        return try {
            val questionResult = questionRepository.getById(questionId)
            if (questionResult.isFailure) return Result.failure(questionResult.exceptionOrNull() ?: Exception("Question not found"))
            
            val question = questionResult.getOrNull() ?: return Result.failure(Exception("Question not found"))
            val isCorrect = question.isCorrectAnswer(selectedAnswer)
            
            val configResult = widgetConfigRepository.getByWidgetId(widgetId)
            val learningSetId = configResult.getOrNull()?.learningSetId ?: ""
            
            val history = com.example.ailearning.domain.model.AnswerHistory(
                questionId = questionId,
                learningSetId = learningSetId,
                selectedAnswer = selectedAnswer,
                isCorrect = isCorrect,
                responseTimeMs = responseTimeMs,
                sessionId = widgetId.toString(),
                source = com.example.ailearning.domain.model.AnswerHistory.SOURCE_WIDGET
            )
            historyRepository.insert(history).getOrThrow()
            
            val config = configResult.getOrNull()!!
            val cachedIds = config.getCachedQuestionIdList()
            val currentIndex = cachedIds.indexOf(questionId)
            val nextQuestion = if (currentIndex >= 0 && currentIndex + 1 < cachedIds.size) {
                questionRepository.getById(cachedIds[currentIndex + 1]).getOrNull()
            } else {
                // 回答統計に基づく重み付き選択（未回答優先・毎回正解の問題は出にくい）
                val candidates = questionRepository.getQuestionsWithStats(config.learningSetId, WIDGET_FETCH_LIMIT)
                    .getOrElse { emptyList() }
                QuestionSelectionPolicy.selectOne(candidates)
            }
            
            Result.success(WidgetAnswerResult(
                isCorrect = isCorrect,
                correctAnswer = question.correctAnswer,
                explanation = question.explanation,
                nextQuestion = nextQuestion
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class WidgetAnswerResult(
    val isCorrect: Boolean,
    val correctAnswer: Int,
    val explanation: String,
    val nextQuestion: Question?
)

class UpdateWidgetConfigUseCase @Inject constructor(
    private val repository: WidgetConfigRepository
) {
    operator suspend fun invoke(config: WidgetConfig): Result<Unit> {
        return repository.update(config).map { Unit }
    }
}

class CreateWidgetConfigUseCase @Inject constructor(
    private val repository: WidgetConfigRepository
) {
    operator suspend fun invoke(widgetId: Int, learningSetId: String): Result<WidgetConfig> {
        return try {
            val config = WidgetConfig(
                widgetId = widgetId,
                learningSetId = learningSetId
            )
            repository.insert(config).getOrThrow()
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteWidgetConfigUseCase @Inject constructor(
    private val repository: WidgetConfigRepository
) {
    operator suspend fun invoke(widgetId: Int): Result<Unit> {
        return try {
            val configResult = repository.getByWidgetId(widgetId)
            if (configResult.isSuccess) {
                val config = configResult.getOrNull()
                if (config != null) {
                    repository.delete(config).getOrThrow()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
