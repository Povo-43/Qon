package com.example.ailearning.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "api_keys")
data class ApiKey(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val provider: String,          // AiProviderCatalog の id
    val model: String,
    val encryptedKey: String,      // 暗号化済みAPIキー
    val baseUrl: String? = null,
    val priority: Int = 0,         // 優先度（高いほど優先）
    val isEnabled: Boolean = true,
    val usageType: String,         // "question_generation" / "analysis" / "both"
    val lastTestedAt: Long = 0,
    val lastTestSuccess: Boolean = false,
    val totalRequests: Long = 0,
    val totalTokensUsed: Long = 0,
    val errorCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val PROVIDER_OPENAI = "openai"
        const val PROVIDER_ANTHROPIC = "anthropic"
        const val PROVIDER_GOOGLE = "google"
        const val PROVIDER_NVIDIA = "nvidia"
        const val PROVIDER_KILO = "kilo"
        const val PROVIDER_OPENROUTER = "openrouter"
        const val PROVIDER_GROQ = "groq"
        const val PROVIDER_DEEPSEEK = "deepseek"
        const val PROVIDER_XAI = "xai"
        const val PROVIDER_MISTRAL = "mistral"
        const val PROVIDER_TOGETHER = "together"
        const val PROVIDER_FIREWORKS = "fireworks"
        const val PROVIDER_PERPLEXITY = "perplexity"
        const val PROVIDER_CEREBRAS = "cerebras"
        const val PROVIDER_SAMBANOVA = "sambanova"
        const val PROVIDER_NEBIUS = "nebius"
        const val PROVIDER_MOONSHOT = "moonshot"
        const val PROVIDER_ZHIPU = "zhipu"
        const val PROVIDER_OLLAMA = AiProvider.ID_OLLAMA
        const val PROVIDER_LMSTUDIO = AiProvider.ID_LMSTUDIO
        const val PROVIDER_CUSTOM = "custom"

        const val USAGE_GENERATION = "question_generation"
        const val USAGE_ANALYSIS = "analysis"
        const val USAGE_BOTH = "both"

        /** カタログに登録された全プロバイダ（表示順）。 */
        val ALL_PROVIDERS: List<String>
            get() = AiProviderCatalog.providers.map { it.id }

        val ALL_USAGE_TYPES = listOf(USAGE_GENERATION, USAGE_ANALYSIS, USAGE_BOTH)
    }

    fun getDisplayName(): String = "$name (${AiProviderCatalog.label(provider)} - $model)"
}
