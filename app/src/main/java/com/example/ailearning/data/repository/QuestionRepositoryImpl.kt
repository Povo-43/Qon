package com.example.ailearning.data.repository

import com.example.ailearning.data.local.db.QuestionDao
import com.example.ailearning.domain.model.Question
import com.example.ailearning.domain.model.QuestionWithStats
import com.example.ailearning.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class QuestionRepositoryImpl @Inject constructor(
    private val dao: QuestionDao
) : QuestionRepository {
    override suspend fun insertAll(questions: List<Question>) = Result.success(dao.insertAll(questions))
    override suspend fun insert(question: Question) = Result.success(dao.insert(question))
    override suspend fun update(question: Question) = Result.success(dao.update(question))
    override fun observeQuestionsWithStats(learningSetId: String): Flow<List<QuestionWithStats>> =
        dao.observeQuestionsWithStats(learningSetId)
    override suspend fun getQuestionsWithStats(learningSetId: String, limit: Int) =
        Result.success(dao.getQuestionsWithStats(learningSetId, limit))
    override suspend fun getRandomQuestions(learningSetId: String, limit: Int) = Result.success(dao.getRandomQuestions(learningSetId, limit))
    override suspend fun getRandomQuestions(learningSetId: String, limit: Int, excludeIds: List<String>) = Result.success(dao.getRandomQuestions(learningSetId, limit, excludeIds))
    override suspend fun getQuestionsByCategory(learningSetId: String, category: String, limit: Int) = Result.success(dao.getQuestionsByCategory(learningSetId, category, limit))
    override suspend fun getQuestionsByCategory(learningSetId: String, category: String, limit: Int, excludeIds: List<String>) = Result.success(dao.getQuestionsByCategory(learningSetId, category, limit, excludeIds))
    override suspend fun getQuestionsByDifficulty(learningSetId: String, difficulty: Int, limit: Int) = Result.success(dao.getQuestionsByDifficulty(learningSetId, difficulty, limit))
    override suspend fun getQuestionsByDifficulty(learningSetId: String, difficulty: Int, limit: Int, excludeIds: List<String>) = Result.success(dao.getQuestionsByDifficulty(learningSetId, difficulty, limit, excludeIds))
    override suspend fun getRecentQuestions(learningSetId: String, limit: Int) = Result.success(dao.getRecentQuestions(learningSetId, limit))
    override suspend fun getWeakQuestions(learningSetId: String, limit: Int) = Result.success(dao.getWeakQuestions(learningSetId, limit))
    override suspend fun getWeakQuestions(learningSetId: String, limit: Int, excludeIds: List<String>) = Result.success(dao.getWeakQuestions(learningSetId, limit, excludeIds))
    override suspend fun getById(id: String) = Result.success(dao.getById(id))
    override fun getAllByLearningSet(learningSetId: String): Flow<List<Question>> = dao.getAllByLearningSet(learningSetId)
    override suspend fun countByLearningSet(learningSetId: String) = Result.success(dao.countByLearningSet(learningSetId))
    override suspend fun findByContentHash(learningSetId: String, hash: String) = Result.success(dao.findByContentHash(learningSetId, hash))
}
