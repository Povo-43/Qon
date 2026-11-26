package com.example.ailearning.domain.usecase

import com.example.ailearning.domain.model.AnswerHistory
import com.example.ailearning.domain.model.LearningSession
import com.example.ailearning.domain.repository.AnswerHistoryRepository
import com.example.ailearning.domain.repository.LearningSessionRepository
import com.example.ailearning.domain.repository.QuestionRepository
import javax.inject.Inject

class StartLearningSessionUseCase @Inject constructor(
    private val sessionRepository: LearningSessionRepository
) {
    operator suspend fun invoke(learningSetId: String, mode: String): Result<LearningSession> {
        return try {
            val session = LearningSession(
                learningSetId = learningSetId,
                mode = mode
            )
            sessionRepository.insert(session).getOrThrow()
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RecordAnswerUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository,
    private val sessionRepository: LearningSessionRepository,
    private val questionRepository: QuestionRepository
) {
    operator suspend fun invoke(
        sessionId: String,
        questionId: String,
        learningSetId: String,
        selectedAnswer: Int,
        responseTimeMs: Long,
        source: String
    ): Result<AnswerHistory> {
        return try {
            val questionResult = questionRepository.getById(questionId)
            if (questionResult.isFailure) return Result.failure(questionResult.exceptionOrNull() ?: Exception("Question not found"))
            
            val question = questionResult.getOrNull() ?: return Result.failure(Exception("Question not found"))
            val isCorrect = question.isCorrectAnswer(selectedAnswer)
            
            val history = AnswerHistory(
                questionId = questionId,
                learningSetId = learningSetId,
                selectedAnswer = selectedAnswer,
                isCorrect = isCorrect,
                responseTimeMs = responseTimeMs,
                sessionId = sessionId,
                source = source
            )
            
            historyRepository.insert(history).getOrThrow()
            
            val sessionResult = sessionRepository.getById(sessionId)
            if (sessionResult.isSuccess) {
                val session = sessionResult.getOrNull()
                if (session != null) {
                    val updatedSession = session.copy(
                        totalQuestions = session.totalQuestions + 1,
                        correctAnswers = session.correctAnswers + (if (isCorrect) 1 else 0),
                        totalTimeMs = session.totalTimeMs + responseTimeMs
                    )
                    sessionRepository.update(updatedSession).getOrThrow()
                }
            }
            
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class EndLearningSessionUseCase @Inject constructor(
    private val sessionRepository: LearningSessionRepository
) {
    operator suspend fun invoke(sessionId: String): Result<Unit> {
        return try {
            val sessionResult = sessionRepository.getById(sessionId)
            if (sessionResult.isSuccess) {
                val session = sessionResult.getOrNull()
                if (session != null) {
                    val endedSession = session.copy(endedAt = System.currentTimeMillis())
                    sessionRepository.update(endedSession).getOrThrow()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetSessionHistoryUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository
) {
    operator suspend fun invoke(sessionId: String): Result<List<AnswerHistory>> {
        return historyRepository.getBySessionId(sessionId)
    }
}

class GetAnsweredQuestionIdsBySessionUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository
) {
    operator suspend fun invoke(sessionId: String): Result<List<String>> {
        return historyRepository.getAnsweredQuestionIdsBySession(sessionId)
    }
}

class GetAnsweredQuestionIdsByLearningSetUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository
) {
    operator suspend fun invoke(learningSetId: String): Result<List<String>> {
        return historyRepository.getAnsweredQuestionIdsByLearningSet(learningSetId)
    }
}

class GetAnsweredQuestionIdsByWidgetIdUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository
) {
    operator suspend fun invoke(widgetId: String): Result<List<String>> {
        return historyRepository.getAnsweredQuestionIdsBySession(widgetId)
    }
}
