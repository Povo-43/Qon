package com.example.ailearning.data.remote.model

import kotlinx.serialization.Serializable

/** OpenAI / Anthropic / OpenRouter など `GET /models` の共通形式。 */
@Serializable
data class ModelListResponse(
    val data: List<ModelListItem> = emptyList()
)

@Serializable
data class ModelListItem(
    val id: String = ""
)

/** Google Gemini `GET models` の形式。 */
@Serializable
data class GeminiModelListResponse(
    val models: List<GeminiModelItem> = emptyList()
)

@Serializable
data class GeminiModelItem(
    val name: String = "",
    val displayName: String = ""
)

private val NON_CHAT_KEYWORDS = listOf(
    "embed",
    "whisper",
    "tts",
    "text-to-speech",
    "dall-e",
    "moderation",
    "rerank",
    "transcribe",
    "audio",
    "image"
)

/**
 * モデル一覧をチャット向けに整形する。
 * 埋め込み・音声・画像生成などの明らかに非チャットなモデルを除外し、重複を除いて並べる。
 */
fun sanitizeChatModelIds(ids: List<String>): List<String> =
    ids.asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filter { id ->
            val lower = id.lowercase()
            NON_CHAT_KEYWORDS.none { keyword -> lower.contains(keyword) }
        }
        .distinct()
        .sortedBy { it.lowercase() }
        .toList()
