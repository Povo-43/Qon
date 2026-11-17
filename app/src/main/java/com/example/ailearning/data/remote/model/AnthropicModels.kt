package com.example.ailearning.data.remote.model

import kotlinx.serialization.Serializable

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
    val role: String,
    val content: String
)

@Serializable
data class AnthropicResponse(
    val content: List<AnthropicContent> = emptyList(),
    val usage: AnthropicUsage? = null
)

@Serializable
data class AnthropicContent(
    val type: String = "text",
    val text: String
)

@Serializable
data class AnthropicUsage(
    val input_tokens: Int = 0,
    val output_tokens: Int = 0
)
