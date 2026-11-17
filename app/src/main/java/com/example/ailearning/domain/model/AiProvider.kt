package com.example.ailearning.domain.model

/**
 * API の通信方式。
 * - OPENAI: OpenAI 互換の /chat/completions
 * - ANTHROPIC: Anthropic Messages API
 * - GEMINI: Google Gemini generateContent API
 */
enum class AiApiStyle {
    OPENAI,
    ANTHROPIC,
    GEMINI
}

/**
 * 接続可能な AI プロバイダの定義。
 * ここに 1 件追加するだけで設定画面・クライアント生成の両方に反映される。
 */
data class AiProvider(
    val id: String,
    val displayName: String,
    val shortName: String,
    val baseUrl: String,
    val apiStyle: AiApiStyle,
    val exampleModel: String,
    val models: List<String> = emptyList(),
    val brandColor: Long = 0xFF6366F1,
    val keyHint: String = "APIキーを入力",
    val description: String = "",
    val requiresBaseUrl: Boolean = false
) {
    val isLocal: Boolean
        get() = id == ID_OLLAMA || id == ID_LMSTUDIO

    companion object {
        const val ID_OLLAMA = "ollama"
        const val ID_LMSTUDIO = "lmstudio"
    }
}

object AiProviderCatalog {

    val providers: List<AiProvider> = listOf(
        AiProvider(
            id = ApiKey.PROVIDER_OPENAI,
            displayName = "OpenAI",
            shortName = "OpenAI",
            baseUrl = "https://api.openai.com/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "gpt-4o-mini",
            models = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "o4-mini"),
            brandColor = 0xFF10A37F,
            keyHint = "sk-...",
            description = "GPT シリーズ。汎用性が高い定番モデル。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_ANTHROPIC,
            displayName = "Anthropic Claude",
            shortName = "Claude",
            baseUrl = "https://api.anthropic.com/v1/",
            apiStyle = AiApiStyle.ANTHROPIC,
            exampleModel = "claude-3-5-sonnet-20241022",
            models = listOf(
                "claude-3-5-sonnet-20241022",
                "claude-3-5-haiku-20241022",
                "claude-3-opus-20240229"
            ),
            brandColor = 0xFFD97757,
            keyHint = "sk-ant-...",
            description = "長文理解と日本語の自然さに強い Claude。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_GOOGLE,
            displayName = "Google Gemini",
            shortName = "Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/",
            apiStyle = AiApiStyle.GEMINI,
            exampleModel = "gemini-1.5-flash",
            models = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash"),
            brandColor = 0xFF4285F4,
            keyHint = "AIza...",
            description = "高速で無料枠もある Google のモデル。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_NVIDIA,
            displayName = "NVIDIA NIM",
            shortName = "NVIDIA",
            baseUrl = "https://integrate.api.nvidia.com/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "meta/llama-3.3-70b-instruct",
            models = listOf(
                "meta/llama-3.3-70b-instruct",
                "meta/llama-3.1-70b-instruct",
                "nvidia/llama-3.1-nemotron-70b-instruct",
                "mistralai/mistral-7b-instruct-v0.3"
            ),
            brandColor = 0xFF76B900,
            keyHint = "nvapi-...",
            description = "NVIDIA のホスト型 NIM。無料トライアル枠あり。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_KILO,
            displayName = "Kilo AI Gateway",
            shortName = "Kilo",
            baseUrl = "https://ai-gateway.kilo.ai/api/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "anthropic/claude-sonnet-4.5",
            models = listOf(
                "anthropic/claude-sonnet-4.5",
                "openai/gpt-4o-mini",
                "google/gemini-2.5-flash",
                "meta-llama/llama-3.3-70b-instruct"
            ),
            brandColor = 0xFF7C3AED,
            keyHint = "kilo-...",
            description = "1 つのキーで数百モデルにアクセスできるゲートウェイ。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_OPENROUTER,
            displayName = "OpenRouter",
            shortName = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "openai/gpt-4o-mini",
            models = listOf(
                "openai/gpt-4o-mini",
                "anthropic/claude-3.5-sonnet",
                "google/gemini-flash-1.5",
                "meta-llama/llama-3.3-70b-instruct"
            ),
            brandColor = 0xFF6467F2,
            keyHint = "sk-or-...",
            description = "多数のモデルを横断できるルーティングサービス。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_GROQ,
            displayName = "Groq",
            shortName = "Groq",
            baseUrl = "https://api.groq.com/openai/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "llama-3.3-70b-versatile",
            models = listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
                "mixtral-8x7b-32768"
            ),
            brandColor = 0xFFF55036,
            keyHint = "gsk_...",
            description = "超高速推論が特徴。軽量モデルが中心。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_DEEPSEEK,
            displayName = "DeepSeek",
            shortName = "DeepSeek",
            baseUrl = "https://api.deepseek.com/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "deepseek-chat",
            models = listOf("deepseek-chat", "deepseek-reasoner"),
            brandColor = 0xFF4D6BFE,
            keyHint = "sk-...",
            description = "低コストで高性能な中国発のモデル。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_XAI,
            displayName = "xAI Grok",
            shortName = "Grok",
            baseUrl = "https://api.x.ai/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "grok-3-mini",
            models = listOf("grok-3-mini", "grok-3", "grok-2-latest"),
            brandColor = 0xFF111111,
            keyHint = "xai-...",
            description = "xAI の Grok シリーズ。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_MISTRAL,
            displayName = "Mistral AI",
            shortName = "Mistral",
            baseUrl = "https://api.mistral.ai/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "mistral-small-latest",
            models = listOf("mistral-small-latest", "mistral-large-latest", "open-mistral-nemo"),
            brandColor = 0xFFFF7000,
            keyHint = "APIキーを入力",
            description = "欧州発のオープンモデル。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_TOGETHER,
            displayName = "Together AI",
            shortName = "Together",
            baseUrl = "https://api.together.xyz/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "meta-llama/Llama-3.3-70B-Instruct-Turbo",
            models = listOf(
                "meta-llama/Llama-3.3-70B-Instruct-Turbo",
                "mistralai/Mixtral-8x7B-Instruct-v0.1",
                "Qwen/Qwen2.5-72B-Instruct-Turbo"
            ),
            brandColor = 0xFF0F6FFF,
            keyHint = "APIキーを入力",
            description = "オープンモデルを多数ホストするサービス。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_FIREWORKS,
            displayName = "Fireworks AI",
            shortName = "Fireworks",
            baseUrl = "https://api.fireworks.ai/inference/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "accounts/fireworks/models/llama-v3p3-70b-instruct",
            models = listOf(
                "accounts/fireworks/models/llama-v3p3-70b-instruct",
                "accounts/fireworks/models/mixtral-8x7b-instruct"
            ),
            brandColor = 0xFF5019C5,
            keyHint = "fw_...",
            description = "高速なオープンモデル推論基盤。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_PERPLEXITY,
            displayName = "Perplexity",
            shortName = "Perplexity",
            baseUrl = "https://api.perplexity.ai/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "sonar",
            models = listOf("sonar", "sonar-pro", "sonar-reasoning"),
            brandColor = 0xFF20808D,
            keyHint = "pplx-...",
            description = "検索連携に強いモデル。JSON 出力は非対応の場合あり。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_CEREBRAS,
            displayName = "Cerebras",
            shortName = "Cerebras",
            baseUrl = "https://api.cerebras.ai/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "llama3.3-70b",
            models = listOf("llama3.3-70b", "llama3.1-8b"),
            brandColor = 0xFFF97316,
            keyHint = "csk-...",
            description = "ウェハースケールの超高速推論。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_SAMBANOVA,
            displayName = "SambaNova",
            shortName = "SambaNova",
            baseUrl = "https://api.sambanova.ai/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "Meta-Llama-3.3-70B-Instruct",
            models = listOf(
                "Meta-Llama-3.3-70B-Instruct",
                "Meta-Llama-3.1-8B-Instruct"
            ),
            brandColor = 0xFFEE7624,
            keyHint = "APIキーを入力",
            description = "高速推論クラウド。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_NEBIUS,
            displayName = "Nebius AI Studio",
            shortName = "Nebius",
            baseUrl = "https://api.studio.nebius.ai/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "meta-llama/Llama-3.3-70B-Instruct",
            models = listOf(
                "meta-llama/Llama-3.3-70B-Instruct",
                "Qwen/Qwen2.5-72B-Instruct",
                "mistralai/Mixtral-8x7B-Instruct-v0.1"
            ),
            brandColor = 0xFF1A73E8,
            keyHint = "APIキーを入力",
            description = "欧州のオープンモデル推論基盤。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_MOONSHOT,
            displayName = "Moonshot Kimi",
            shortName = "Kimi",
            baseUrl = "https://api.moonshot.ai/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "moonshot-v1-8k",
            models = listOf("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k"),
            brandColor = 0xFF1B1B1F,
            keyHint = "sk-...",
            description = "長文コンテキストに強い Kimi。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_ZHIPU,
            displayName = "Zhipu GLM",
            shortName = "GLM",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "glm-4-flash",
            models = listOf("glm-4-flash", "glm-4-plus", "glm-4-air"),
            brandColor = 0xFF3859FF,
            keyHint = "APIキーを入力",
            description = "中国の GLM シリーズ。"
        ),
        AiProvider(
            id = AiProvider.ID_OLLAMA,
            displayName = "Ollama（ローカル）",
            shortName = "Ollama",
            baseUrl = "http://10.0.2.2:11434/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "llama3.2",
            models = listOf("llama3.2", "qwen2.5", "gemma2", "phi3"),
            brandColor = 0xFF111827,
            keyHint = "ollama（不要なら空欄可）",
            description = "PC 上の Ollama に接続。エミュレータからは 10.0.2.2 を使用。"
        ),
        AiProvider(
            id = AiProvider.ID_LMSTUDIO,
            displayName = "LM Studio（ローカル）",
            shortName = "LM Studio",
            baseUrl = "http://10.0.2.2:1234/v1/",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "local-model",
            models = listOf("local-model"),
            brandColor = 0xFF0EA5E9,
            keyHint = "lm-studio（不要なら空欄可）",
            description = "PC 上の LM Studio に接続。OpenAI 互換サーバーを有効化してください。"
        ),
        AiProvider(
            id = ApiKey.PROVIDER_CUSTOM,
            displayName = "OpenAI 互換（カスタム）",
            shortName = "カスタム",
            baseUrl = "",
            apiStyle = AiApiStyle.OPENAI,
            exampleModel = "",
            brandColor = 0xFF6366F1,
            keyHint = "APIキーを入力",
            description = "任意の OpenAI 互換エンドポイント。ベースURLの指定が必須です。",
            requiresBaseUrl = true
        )
    )

    val byId: Map<String, AiProvider> = providers.associateBy { it.id }

    fun find(id: String?): AiProvider? = id?.let { byId[it.lowercase()] }

    fun getOrNull(id: String?): AiProvider? = find(id)

    /** 未知の ID はカスタム（OpenAI 互換）として扱う。 */
    fun resolve(id: String?): AiProvider =
        find(id) ?: byId.getValue(ApiKey.PROVIDER_CUSTOM)

    fun label(id: String?): String = find(id)?.displayName ?: (id ?: "不明")

    fun shortLabel(id: String?): String = find(id)?.shortName ?: (id ?: "不明")

    fun brandColor(id: String?): Long = find(id)?.brandColor ?: 0xFF6366F1
}
