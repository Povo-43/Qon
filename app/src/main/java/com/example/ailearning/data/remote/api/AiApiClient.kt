package com.example.ailearning.data.remote.api

import com.example.ailearning.domain.model.AnalyzeLearningRequest
import com.example.ailearning.domain.model.AnalyzeLearningResponse
import com.example.ailearning.domain.model.GenerateQuestionsRequest
import com.example.ailearning.domain.model.GenerateQuestionsResponse

interface AiApiClient {
    suspend fun generateQuestions(request: GenerateQuestionsRequest): Result<GenerateQuestionsResponse>
    suspend fun analyzeLearning(request: AnalyzeLearningRequest): Result<AnalyzeLearningResponse>
    suspend fun testConnection(): Result<Unit>

    /** プロバイダのモデル一覧を取得する。未対応の場合は失敗を返す。 */
    suspend fun listModels(): Result<List<String>> =
        Result.failure(UnsupportedOperationException("このプロバイダはモデル一覧取得に対応していません"))

    val providerName: String
    val supportedModels: List<String>
}
