package com.example.ailearning.domain.repository

import com.example.ailearning.domain.model.AnalyzeLearningRequest
import com.example.ailearning.domain.model.AnalyzeLearningResponse
import com.example.ailearning.domain.model.GenerateQuestionsRequest
import com.example.ailearning.domain.model.GenerateQuestionsResponse

interface AiRepository {
    suspend fun generateQuestions(request: GenerateQuestionsRequest): Result<GenerateQuestionsResponse>
    suspend fun analyzeLearning(request: AnalyzeLearningRequest): Result<AnalyzeLearningResponse>
    suspend fun testConnection(provider: String, apiKey: String, model: String, baseUrl: String?): Result<Unit>
}
