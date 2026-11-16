package com.example.ailearning.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GenerateQuestionsRequest(
    val learningSet: LearningSet,
    val count: Int = 10,
    val difficultyStart: Int = 1,
    val difficultyEnd: Int = 5,
    val categories: List<String> = emptyList(),
    val excludeQuestionIds: List<String> = emptyList(),
    val previousQuestions: List<QuestionSummary> = emptyList()
)

@Serializable
data class QuestionSummary(
    val questionText: String,
    val category: String,
    val difficulty: Int
)

@Serializable
data class GenerateQuestionsResponse(
    val questions: List<GeneratedQuestion>,
    val modelUsed: String,
    val tokensUsed: Int
)

@Serializable
data class GeneratedQuestion(
    val questionText: String,
    val options: List<String>,  // 4つ
    val correctAnswer: Int,     // 0-3
    val explanation: String,
    val difficulty: Int,        // 1-5
    val category: String
) {
    fun toQuestion(learningSetId: String, modelUsed: String): Question {
        return Question(
            learningSetId = learningSetId,
            questionText = questionText,
            optionA = options.getOrNull(0) ?: "",
            optionB = options.getOrNull(1) ?: "",
            optionC = options.getOrNull(2) ?: "",
            optionD = options.getOrNull(3) ?: "",
            correctAnswer = correctAnswer,
            explanation = explanation,
            difficulty = difficulty.coerceIn(Question.DIFFICULTY_MIN, Question.DIFFICULTY_MAX),
            category = category,
            source = Question.SOURCE_AI,
            aiModelUsed = modelUsed,
            duplicateCheck = DuplicateCheck(
                contentHash = questionText.hashCode().toString(),
                optionsHash = options.joinToString("|").hashCode().toString()
            )
        )
    }
}

@Serializable
data class AnalyzeLearningRequest(
    val learningSet: LearningSet,
    val recentHistories: List<AnswerHistory>,
    val categoryStats: List<CategoryStatistics>,
    val difficultyStats: List<DifficultyStatistics>,
    val analysisType: String  // "weakness" / "trend" / "recommendation"
)

@Serializable
data class AnalyzeLearningResponse(
    val analysis: String,       // JSON形式
    val modelUsed: String,
    val tokensUsed: Int
)

// OpenAI API Models
@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int? = null,
    val response_format: OpenAiResponseFormat? = null
)

@Serializable
data class OpenAiMessage(
    val role: String,  // "system" / "user" / "assistant"
    val content: String
)

@Serializable
data class OpenAiResponseFormat(
    val type: String = "json_object"
)

@Serializable
data class OpenAiChatResponse(
    val id: String,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage
)

@Serializable
data class OpenAiChoice(
    val message: OpenAiMessage,
    val finish_reason: String
)

@Serializable
data class OpenAiUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

// Anthropic API Models
@Serializable
data class AnthropicRequest(
    val model: String,
    val max_tokens: Int,
    val messages: List<AnthropicMessage>,
    val system: String? = null,
    val temperature: Double = 0.7
)

@Serializable
data class AnthropicMessage(
    val role: String,  // "user" / "assistant"
    val content: String
)

@Serializable
data class AnthropicResponse(
    val id: String,
    val content: List<AnthropicContent>,
    val usage: AnthropicUsage
)

@Serializable
data class AnthropicContent(
    val type: String,  // "text"
    val text: String
)

@Serializable
data class AnthropicUsage(
    val input_tokens: Int,
    val output_tokens: Int
)

// Google Gemini API Models
@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val safetySettings: List<GeminiSafetySetting>? = null
)

@Serializable
data class GeminiContent(
    val role: String,  // "user" / "model"
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double = 0.7,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String = "application/json"
)

@Serializable
data class GeminiSafetySetting(
    val category: String,
    val threshold: String
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>,
    val usageMetadata: GeminiUsageMetadata?
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent,
    val finishReason: String
)

@Serializable
data class GeminiUsageMetadata(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int
)
