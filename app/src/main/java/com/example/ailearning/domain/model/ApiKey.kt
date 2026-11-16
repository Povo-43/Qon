package com.example.ailearning.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "api_keys")
data class ApiKey(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val provider: String,          // "openai" / "anthropic" / "google" / "custom"
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
        const val PROVIDER_CUSTOM = "custom"
        
        const val USAGE_GENERATION = "question_generation"
        const val USAGE_ANALYSIS = "analysis"
        const val USAGE_BOTH = "both"
        
        val ALL_PROVIDERS = listOf(PROVIDER_OPENAI, PROVIDER_ANTHROPIC, PROVIDER_GOOGLE, PROVIDER_CUSTOM)
        val ALL_USAGE_TYPES = listOf(USAGE_GENERATION, USAGE_ANALYSIS, USAGE_BOTH)
    }
    
    fun getDisplayName(): String = "$name ($provider - $model)"
}
