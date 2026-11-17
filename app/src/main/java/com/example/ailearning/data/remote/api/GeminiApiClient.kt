package com.example.ailearning.data.remote.api

import com.example.ailearning.data.remote.model.GeminiRequest
import com.example.ailearning.data.remote.model.GeminiResponse
import com.example.ailearning.data.remote.model.GeminiContent
import com.example.ailearning.data.remote.model.GeminiPart
import com.example.ailearning.data.remote.model.GeminiGenerationConfig
import com.example.ailearning.data.remote.model.GeminiModelListResponse
import com.example.ailearning.data.remote.model.sanitizeChatModelIds
import com.example.ailearning.domain.model.AnalyzeLearningRequest
import com.example.ailearning.domain.model.AnalyzeLearningResponse
import com.example.ailearning.domain.model.GenerateQuestionsRequest
import com.example.ailearning.domain.model.GenerateQuestionsResponse
import com.example.ailearning.domain.model.GeneratedQuestion
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Named

class GeminiApiClient @Inject constructor(
    @Named("io_dispatcher") private val ioDispatcher: CoroutineDispatcher,
    private val apiKey: String,
    private val model: String,
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta/"
) : AiApiClient {
    
    override val providerName = "google"
    override val supportedModels = listOf("gemini-1.5-pro", "gemini-1.5-flash")
    
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BaseUrlNormalizer.forGemini(baseUrl))
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            )
            .build()
    }
    
    private val service = retrofit.create(GeminiService::class.java)
    private val gson = Gson()

    private val generateEndpoint: String
        get() = BaseUrlNormalizer.forGemini(baseUrl) +
            "models/${model.removePrefix("models/")}:generateContent"

    private val modelsEndpoint: String
        get() = BaseUrlNormalizer.forGemini(baseUrl) + "models"
    
    override suspend fun generateQuestions(request: GenerateQuestionsRequest): Result<GenerateQuestionsResponse> = withContext(ioDispatcher) {
        try {
            val prompt = buildPrompt(request)
            
            val contents = listOf(
                GeminiContent("user", listOf(GeminiPart(prompt)))
            )
            
            val req = GeminiRequest(
                contents = contents,
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7,
                    maxOutputTokens = 4000,
                    responseMimeType = "application/json"
                )
            )
            
            val response = service.generateContent("models/${model.removePrefix("models/")}:generateContent?key=$apiKey", req)
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception(
                        ApiErrorDetails.http(
                            action = "問題生成",
                            provider = "google",
                            model = model,
                            endpoint = generateEndpoint,
                            code = response.code(),
                            body = response.errorBody()?.string()
                        )
                    )
                )
            }
            
            val body = response.body() ?: return@withContext Result.failure(Exception("Empty response"))
            val candidate = body.candidates.firstOrNull() ?: return@withContext Result.failure(Exception("No candidates"))
            val content = candidate.content.parts.firstOrNull()?.text ?: return@withContext Result.failure(Exception("No content"))
            
            val json = JsonParser.parseString(content).asJsonObject
            val questionsJson = json.getAsJsonArray("questions")
            
            val questions = questionsJson.asSequence()
                .map { it.asJsonObject }
                .map { parseGeneratedQuestion(it) }
                .filterNotNull()
                .toList()
            
            val tokensUsed = body.usageMetadata?.totalTokenCount ?: 0
            
            Result.success(GenerateQuestionsResponse(
                questions = questions,
                modelUsed = model,
                tokensUsed = tokensUsed
            ))
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    ApiErrorDetails.exception("問題生成", "google", model, generateEndpoint, e),
                    e
                )
            )
        }
    }
    
    override suspend fun analyzeLearning(request: AnalyzeLearningRequest): Result<AnalyzeLearningResponse> = withContext(ioDispatcher) {
        try {
            val prompt = buildAnalysisPrompt(request)
            
            val contents = listOf(
                GeminiContent("user", listOf(GeminiPart(prompt)))
            )
            
            val req = GeminiRequest(
                contents = contents,
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.5,
                    maxOutputTokens = 2000,
                    responseMimeType = "application/json"
                )
            )
            
            val response = service.generateContent("models/${model.removePrefix("models/")}:generateContent?key=$apiKey", req)
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception(
                        ApiErrorDetails.http(
                            action = "学習分析",
                            provider = "google",
                            model = model,
                            endpoint = generateEndpoint,
                            code = response.code(),
                            body = response.errorBody()?.string()
                        )
                    )
                )
            }
            
            val body = response.body() ?: return@withContext Result.failure(Exception("Empty response"))
            val candidate = body.candidates.firstOrNull() ?: return@withContext Result.failure(Exception("No candidates"))
            val content = candidate.content.parts.firstOrNull()?.text ?: return@withContext Result.failure(Exception("No content"))
            
            val tokensUsed = body.usageMetadata?.totalTokenCount ?: 0
            
            Result.success(AnalyzeLearningResponse(
                analysis = content,
                modelUsed = model,
                tokensUsed = tokensUsed
            ))
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    ApiErrorDetails.exception("学習分析", "google", model, generateEndpoint, e),
                    e
                )
            )
        }
    }
    
    override suspend fun testConnection(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val contents = listOf(GeminiContent("user", listOf(GeminiPart("Hello"))))
            val req = GeminiRequest(contents = contents)
            val response = service.generateContent("models/${model.removePrefix("models/")}:generateContent?key=$apiKey", req)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception(
                        ApiErrorDetails.http(
                            action = "接続テスト",
                            provider = "google",
                            model = model,
                            endpoint = generateEndpoint,
                            code = response.code(),
                            body = response.errorBody()?.string()
                        )
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    ApiErrorDetails.exception("接続テスト", "google", model, generateEndpoint, e),
                    e
                )
            )
        }
    }
    
    override suspend fun listModels(): Result<List<String>> = withContext(ioDispatcher) {
        try {
            val response = service.listModels(apiKey)
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception(
                        ApiErrorDetails.http(
                            action = "モデル一覧の取得",
                            provider = "google",
                            model = model,
                            endpoint = modelsEndpoint,
                            code = response.code(),
                            body = response.errorBody()?.string()
                        )
                    )
                )
            }
            val body = response.body() ?: return@withContext Result.failure(Exception("Empty response"))
            val models = sanitizeChatModelIds(
                body.models.map { it.name.removePrefix("models/") }
            )
            if (models.isEmpty()) {
                Result.failure(Exception("利用可能なモデルが見つかりませんでした"))
            } else {
                Result.success(models)
            }
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    ApiErrorDetails.exception("モデル一覧の取得", "google", model, modelsEndpoint, e),
                    e
                )
            )
        }
    }

    private fun buildPrompt(request: GenerateQuestionsRequest): String {
        val previous = request.previousQuestions.joinToString("\n") { "- ${it.questionText} (${it.category}, 難易度${it.difficulty})" }
        val exclude = request.excludeQuestionIds.joinToString(", ")
        return """
            あなたは優秀な問題作成AIです。以下の条件で4択問題を作成してください。
            
            教科: ${request.learningSet.subject}
            科目: ${request.learningSet.category}
            単元: ${request.learningSet.unit}
            学習範囲: ${request.learningSet.scope}
            
            条件:
            - 難易度: ${request.difficultyStart}〜${request.difficultyEnd} (1:易しい〜5:難しい)
            - 問題数: ${request.count}問
            ${if (request.categories.isNotEmpty()) "- 対象分野: ${request.categories.joinToString(", ")}\n" else ""}
            ${if (previous.isNotBlank()) "既出問題（重複しないよう注意）:\n$previous\n" else ""}
            ${if (exclude.isNotBlank()) "除外ID: $exclude\n" else ""}
            
            以下のJSON形式で出力してください:
            {
              "questions": [
                {
                  "questionText": "問題文",
                  "options": ["選択肢A", "選択肢B", "選択肢C", "選択肢D"],
                  "correctAnswer": 0,
                  "explanation": "解説",
                  "difficulty": 3,
                  "category": "分野名"
                }
              ]
            }
        """.trimIndent()
    }
    
    private fun buildAnalysisPrompt(request: AnalyzeLearningRequest): String {
        val historySummary = request.recentHistories.take(20).joinToString("\n") {
            "Q: ${it.questionId} | 正誤: ${if (it.isCorrect) "○" else "×"} | 時間: ${it.responseTimeMs}ms | 来源: ${it.source}"
        }
        
        val categoryStats = request.categoryStats.joinToString("\n") {
            "- ${it.category}: 正答率 ${"%.1f".format(it.accuracy * 100)}% (${it.correct}/${it.total}) 平均${"%.1f".format(it.avgResponseTimeMs / 1000)}秒"
        }
        
        val difficultyStats = request.difficultyStats.joinToString("\n") {
            "- 難易度${it.difficulty}: 正答率 ${"%.1f".format(it.accuracy * 100)}% (${it.correct}/${it.total})"
        }
        
        return """
            学習セット: ${request.learningSet.name} (${request.learningSet.subject} - ${request.learningSet.category})
            分析タイプ: ${request.analysisType}
            
            最近の回答履歴 (最新20件):
            $historySummary
            
            単元別統計:
            $categoryStats
            
            難易度別統計:
            $difficultyStats
            
            以下のJSON形式で分析結果を出力してください:
            {
              "summary": "全体のサマリー",
              "weakCategories": [
                {"category": "分野名", "accuracy": 0.35, "totalAttempts": 20, "suggestion": "改善提案"}
              ],
              "recommendations": ["おすすめ1", "おすすめ2"],
              "nextFocusAreas": ["重点分野1", "重点分野2"]
            }
        """.trimIndent()
    }
    
    private fun parseGeneratedQuestion(json: JsonObject): GeneratedQuestion? {
        return try {
            GeneratedQuestion(
                questionText = json.get("questionText").asString,
                options = json.get("options").asJsonArray.asSequence().map { it.asString }.toList(),
                correctAnswer = json.get("correctAnswer").asInt,
                explanation = json.get("explanation").asString,
                difficulty = json.get("difficulty").asInt.coerceIn(1, 5),
                category = json.get("category").asString
            )
        } catch (e: Exception) {
            null
        }
    }
}

interface GeminiService {
    @retrofit2.http.POST
    suspend fun generateContent(
        @retrofit2.http.Url url: String,
        @retrofit2.http.Body request: GeminiRequest
    ): retrofit2.Response<GeminiResponse>

    @retrofit2.http.GET("models")
    suspend fun listModels(
        @retrofit2.http.Query("key") key: String
    ): retrofit2.Response<GeminiModelListResponse>
}
