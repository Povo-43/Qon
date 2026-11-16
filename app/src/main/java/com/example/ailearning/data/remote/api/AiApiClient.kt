package com.example.ailearning.data.remote.api

import com.example.ailearning.domain.model.AnalyzeLearningRequest
import com.example.ailearning.domain.model.AnalyzeLearningResponse
import com.example.ailearning.domain.model.GenerateQuestionsRequest
import com.example.ailearning.domain.model.GenerateQuestionsResponse

interface AiApiClient {
    suspend fun generateQuestions(request: GenerateQuestionsRequest): Result<GenerateQuestionsResponse>
    suspend fun analyzeLearning(request: AnalyzeLearningRequest): Result<AnalyzeLearningResponse>
    suspend fun testConnection(): Result<Unit>
    val providerName: String
    val supportedModels: List<String>
}
