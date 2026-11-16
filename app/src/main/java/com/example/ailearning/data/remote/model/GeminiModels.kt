package com.example.ailearning.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double = 0.7,
    val maxOutputTokens: Int = 4000,
    val responseMimeType: String = "text/plain"
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>,
    val usageMetadata: GeminiUsageMetadata? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent
)

@Serializable
data class GeminiUsageMetadata(
    val totalTokenCount: Int = 0
)
