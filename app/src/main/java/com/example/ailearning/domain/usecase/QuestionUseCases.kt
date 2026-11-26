package com.example.ailearning.domain.usecase

import com.example.ailearning.domain.model.Question
import com.example.ailearning.domain.repository.AiRepository
import com.example.ailearning.domain.repository.LearningSetRepository
import com.example.ailearning.domain.repository.QuestionRepository
import com.example.ailearning.domain.model.GenerateQuestionsRequest
import javax.inject.Inject

class GenerateQuestionsUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val questionRepository: QuestionRepository,
    private val learningSetRepository: LearningSetRepository
) {
    operator suspend fun invoke(learningSetId: String, count: Int = 10): Result<List<Question>> {
        return invoke(learningSetId, count, emptyList())
    }
    
    operator suspend fun invoke(learningSetId: String, count: Int, excludeIds: List<String>): Result<List<Question>> {
        return try {
            val learningSetResult = learningSetRepository.getByIdOnce(learningSetId)
            if (learningSetResult.isFailure) return Result.failure(learningSetResult.exceptionOrNull() ?: Exception("Learning set not found"))
            
            val learningSet = learningSetResult.getOrNull() ?: return Result.failure(Exception("Learning set not found"))
            
            val existingQuestions = questionRepository.getRecentQuestions(learningSetId, 50).getOrElse { emptyList() }
            val recentExcludeIds = existingQuestions.map { it.id }
            val allExcludeIds = (recentExcludeIds + excludeIds).distinct()
            val previousQuestions = existingQuestions.take(20).map { q ->
                com.example.ailearning.domain.model.QuestionSummary(q.questionText, q.category, q.difficulty)
            }
            
            val request = GenerateQuestionsRequest(
                learningSet = learningSet,
                count = count,
                excludeQuestionIds = allExcludeIds,
                previousQuestions = previousQuestions
            )
            
            val response = aiRepository.generateQuestions(request).getOrThrow()

            val generated = response.questions
            if (generated.isEmpty()) {
                return Result.failure(
                    Exception(
                        "AIから問題を取得できませんでした（応答の解析に失敗）。\n" +
                            "モデルを変更するか、もう一度お試しください。"
                    )
                )
            }

            val filteredQuestions = generated
                .map { it.toQuestion(learningSetId, response.modelUsed) }
                .filter { q ->
                    questionRepository.findByContentHash(learningSetId, q.getContentHash()).getOrNull() == null
                }

            if (filteredQuestions.isEmpty()) {
                return Result.failure(
                    Exception(
                        "生成された${generated.size}問はすべて既存の問題と重複していました。\n" +
                            "もう一度生成するか、学習範囲を変えてみてください。"
                    )
                )
            }

            questionRepository.insertAll(filteredQuestions).getOrThrow()

            Result.success(filteredQuestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * ランダム出題。単純なランダムではなく、回答統計に基づく重み付き選択を行う。
 *
 * - 未回答の問題を最優先で出す
 * - 誤答の多い問題はそれなりに出る（復習価値があるため）
 * - 毎回正解している問題はほとんど出ない
 *
 * @param excludeIds 除外する問題ID（セッション内で回答済みなど）。統計による重み付けは別途効くため、
 *                    過去すべての回答済み問題を渡す必要はない。
 */
class GetRandomQuestionsUseCase @Inject constructor(
    private val repository: QuestionRepository
) {
    operator suspend fun invoke(learningSetId: String, limit: Int): Result<List<Question>> {
        return invoke(learningSetId, limit, emptyList())
    }

    operator suspend fun invoke(learningSetId: String, limit: Int, excludeIds: List<String>): Result<List<Question>> {
        return try {
            val candidates = repository.getQuestionsWithStats(learningSetId, FETCH_LIMIT)
                .getOrElse { emptyList() }
            Result.success(QuestionSelectionPolicy.select(candidates, limit, excludeIds.toSet()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        /** 候補として引く最大件数（ローカルDBなので十分な数を確保する） */
        const val FETCH_LIMIT = 1000
    }
}

class GetQuestionsByCategoryUseCase @Inject constructor(
    private val repository: QuestionRepository
) {
    operator suspend fun invoke(learningSetId: String, category: String, limit: Int): Result<List<Question>> {
        return repository.getQuestionsByCategory(learningSetId, category, limit)
    }
    
    operator suspend fun invoke(learningSetId: String, category: String, limit: Int, excludeIds: List<String>): Result<List<Question>> {
        return repository.getQuestionsByCategory(learningSetId, category, limit, excludeIds)
    }
}

class GetQuestionsByDifficultyUseCase @Inject constructor(
    private val repository: QuestionRepository
) {
    operator suspend fun invoke(learningSetId: String, difficulty: Int, limit: Int): Result<List<Question>> {
        return repository.getQuestionsByDifficulty(learningSetId, difficulty, limit)
    }
    
    operator suspend fun invoke(learningSetId: String, difficulty: Int, limit: Int, excludeIds: List<String>): Result<List<Question>> {
        return repository.getQuestionsByDifficulty(learningSetId, difficulty, limit, excludeIds)
    }
}

class GetWeakQuestionsUseCase @Inject constructor(
    private val repository: QuestionRepository
) {
    operator suspend fun invoke(learningSetId: String, limit: Int): Result<List<Question>> {
        return repository.getWeakQuestions(learningSetId, limit)
    }
    
    operator suspend fun invoke(learningSetId: String, limit: Int, excludeIds: List<String>): Result<List<Question>> {
        return repository.getWeakQuestions(learningSetId, limit, excludeIds)
    }
}

class GetQuestionByIdUseCase @Inject constructor(
    private val repository: QuestionRepository
) {
    operator suspend fun invoke(id: String): Result<Question?> {
        return repository.getById(id)
    }
}

class CheckDuplicateQuestionUseCase @Inject constructor(
    private val repository: QuestionRepository
) {
    operator suspend fun invoke(learningSetId: String, question: Question): Boolean {
        return repository.findByContentHash(learningSetId, question.getContentHash()).getOrNull() != null
    }
}
