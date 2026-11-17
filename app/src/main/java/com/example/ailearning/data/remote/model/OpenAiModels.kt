package com.example.ailearning.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.7,
    val response_format: OpenAiResponseFormat? = null,
    val max_tokens: Int? = null
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiResponseFormat(
    val type: String
)

@Serializable
data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList(),
    val usage: OpenAiUsage? = null
)

@Serializable
data class OpenAiChoice(
    val message: OpenAiMessage
)

@Serializable
data class OpenAiUsage(
    val total_tokens: Int = 0
)
