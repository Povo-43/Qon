package com.example.ailearning.domain.repository

import com.example.ailearning.domain.model.Question
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    suspend fun insertAll(questions: List<Question>): Result<List<Long>>
    suspend fun insert(question: Question): Result<Long>
    suspend fun update(question: Question): Result<Int>
    suspend fun getRandomQuestions(learningSetId: String, limit: Int): Result<List<Question>>
    suspend fun getQuestionsByCategory(learningSetId: String, category: String, limit: Int): Result<List<Question>>
    suspend fun getQuestionsByDifficulty(learningSetId: String, difficulty: Int, limit: Int): Result<List<Question>>
    suspend fun getRecentQuestions(learningSetId: String, limit: Int): Result<List<Question>>
    suspend fun getWeakQuestions(learningSetId: String, limit: Int): Result<List<Question>>
    suspend fun getById(id: String): Result<Question?>
    fun getAllByLearningSet(learningSetId: String): Flow<List<Question>>
    suspend fun countByLearningSet(learningSetId: String): Result<Int>
    suspend fun findByContentHash(learningSetId: String, hash: String): Result<Question?>
}
