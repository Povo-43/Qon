package com.example.ailearning.data.remote.api

import com.example.ailearning.data.local.encryption.EncryptionManager
import com.example.ailearning.domain.model.AiApiStyle
import com.example.ailearning.domain.model.AiProviderCatalog
import com.example.ailearning.domain.model.ApiKey
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named

/**
 * ApiKey の provider / baseUrl から適切なクライアントを生成する。
 * 未知の provider や「OpenAI 互換（カスタム）」は OpenAI 互換として扱う。
 */
class AiClientFactory @Inject constructor(
    @Named("io_dispatcher") private val ioDispatcher: CoroutineDispatcher,
    private val encryptionManager: EncryptionManager,
    private val openAiClient: OpenAiApiClient,
    private val anthropicClient: AnthropicApiClient,
    private val geminiClient: GeminiApiClient
) {
    fun createClient(apiKey: ApiKey): AiApiClient {
        val decryptedKey = encryptionManager.getApiKey(apiKey.id).orEmpty()
        return buildClient(
            providerId = apiKey.provider,
            apiKey = decryptedKey,
            model = apiKey.model,
            baseUrl = apiKey.baseUrl
        )
    }

    fun createClientForProvider(
        provider: String,
        apiKey: String,
        model: String,
        baseUrl: String?
    ): AiApiClient = buildClient(provider, apiKey, model, baseUrl)

    private fun buildClient(
        providerId: String,
        apiKey: String,
        model: String,
        baseUrl: String?
    ): AiApiClient {
        val provider = AiProviderCatalog.resolve(providerId)
        val resolvedBaseUrl = resolveBaseUrl(provider.baseUrl, baseUrl)

        return when (provider.apiStyle) {
            AiApiStyle.ANTHROPIC -> AnthropicApiClient(
                ioDispatcher,
                apiKey,
                model,
                BaseUrlNormalizer.forAnthropic(resolvedBaseUrl)
            )
            AiApiStyle.GEMINI -> GeminiApiClient(
                ioDispatcher,
                apiKey,
                model,
                BaseUrlNormalizer.forGemini(resolvedBaseUrl)
            )
            AiApiStyle.OPENAI -> OpenAiApiClient(
                ioDispatcher,
                apiKey,
                model,
                BaseUrlNormalizer.forOpenAiCompatible(resolvedBaseUrl),
                provider.id
            )
        }
    }

    private fun resolveBaseUrl(defaultUrl: String, userUrl: String?): String {
        val custom = userUrl?.trim().orEmpty()
        return when {
            custom.isNotEmpty() -> custom
            defaultUrl.isNotBlank() -> defaultUrl
            else -> DEFAULT_OPENAI_BASE_URL
        }
    }

    companion object {
        const val DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1/"
    }
}

/**
 * Retrofit は baseUrl が `/` で終わっていることを要求する。
 * ユーザーがエンドポイント全体（例: .../v1/chat/completions）を貼り付けた場合も吸収する。
 */
object BaseUrlNormalizer {

    private val endpointSuffixes = listOf(
        "/chat/completions",
        "/completions",
        "/messages",
        "/responses",
        "/embeddings",
        "/models"
    )

    fun forOpenAiCompatible(raw: String): String = normalize(raw) { url ->
        var result = url
        for (suffix in endpointSuffixes) {
            if (result.endsWith(suffix)) {
                result = result.removeSuffix(suffix)
                break
            }
        }
        // 一部ゲートウェイは /openai/v1 のようにベースが深いので、そのまま尊重する
        result
    }

    fun forAnthropic(raw: String): String = normalize(raw) { url ->
        url.removeSuffix("/messages")
    }

    fun forGemini(raw: String): String = normalize(raw) { url ->
        var result = url
        val marker = result.indexOf(":generateContent")
        if (marker >= 0) result = result.substring(0, marker)
        // .../models/<model> まで貼られた場合は models/ より前をベースにする
        val modelsIndex = result.indexOf("/models/")
        if (modelsIndex >= 0) result = result.substring(0, modelsIndex)
        result
    }

    private fun normalize(raw: String, transform: (String) -> String): String {
        var url = raw.trim()
        if (url.isEmpty()) return url

        // クエリ・フラグメントを除去
        url = url.substringBefore('?').substringBefore('#')
        url = transform(url)

        if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true)
        ) {
            // ローカルホスト系は http、それ以外は https を補完
            val isLocal = url.startsWith("localhost") ||
                url.startsWith("127.0.0.1") ||
                url.startsWith("10.0.2.2") ||
                url.startsWith("192.168.") ||
                url.startsWith("0.0.0.0")
            url = if (isLocal) "http://$url" else "https://$url"
        }

        if (!url.endsWith("/")) url = "$url/"
        return url
    }
}
