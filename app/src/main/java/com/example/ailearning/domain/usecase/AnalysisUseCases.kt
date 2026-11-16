package com.example.ailearning.domain.usecase

import com.example.ailearning.domain.model.AiAnalysis
import com.example.ailearning.domain.model.AnalyzeLearningRequest
import com.example.ailearning.domain.model.AnswerHistory
import com.example.ailearning.domain.model.CategoryStatistics
import com.example.ailearning.domain.model.DifficultyStatistics
import com.example.ailearning.domain.model.LearningSet
import com.example.ailearning.domain.repository.AiAnalysisRepository
import com.example.ailearning.domain.repository.AiRepository
import com.example.ailearning.domain.repository.AnswerHistoryRepository
import com.example.ailearning.domain.repository.LearningSetRepository
import com.example.ailearning.domain.repository.QuestionRepository
import javax.inject.Inject

class AnalyzeLearningUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val analysisRepository: AiAnalysisRepository,
    private val historyRepository: AnswerHistoryRepository,
    private val learningSetRepository: LearningSetRepository,
    private val questionRepository: QuestionRepository
) {
    operator suspend fun invoke(learningSetId: String, type: String): Result<AiAnalysis> {
        return try {
            val learningSetResult = learningSetRepository.getByIdOnce(learningSetId)
            if (learningSetResult.isFailure) return Result.failure(learningSetResult.exceptionOrNull() ?: Exception("Learning set not found"))
            
            val learningSet = learningSetResult.getOrNull() ?: return Result.failure(Exception("Learning set not found"))
            
            val histories = historyRepository.getRecentByLearningSet(learningSetId, 100).getOrElse { emptyList() }
            val categoryStats = historyRepository.getCategoryStats(learningSetId, 0).getOrElse { emptyList() }
            val difficultyStats = historyRepository.getDifficultyStats(learningSetId, 0).getOrElse { emptyList() }
            
            if (histories.size < 5) {
                return Result.failure(Exception("Not enough data for analysis"))
            }
            
            val request = AnalyzeLearningRequest(
                learningSet = learningSet,
                recentHistories = histories,
                categoryStats = categoryStats,
                difficultyStats = difficultyStats,
                analysisType = type
            )
            
            val response = aiRepository.analyzeLearning(request).getOrThrow()
            
            val analysis = AiAnalysis(
                learningSetId = learningSetId,
                analysisType = type,
                content = response.analysis,
                basedOnHistoryCount = histories.size
            )
            
            analysisRepository.insert(analysis).getOrThrow()
            
            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetLatestAnalysisUseCase @Inject constructor(
    private val repository: AiAnalysisRepository
) {
    operator suspend fun invoke(learningSetId: String, type: String): Result<AiAnalysis?> {
        return repository.getLatestByType(learningSetId, type)
    }
}
