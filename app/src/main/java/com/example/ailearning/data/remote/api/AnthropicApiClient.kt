package com.example.ailearning.data.remote.api

import com.example.ailearning.data.remote.model.AnthropicRequest
import com.example.ailearning.data.remote.model.AnthropicResponse
import com.example.ailearning.data.remote.model.AnthropicMessage
import com.example.ailearning.data.remote.model.ModelListResponse
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Named

class AnthropicApiClient @Inject constructor(
    @Named("io_dispatcher") private val ioDispatcher: CoroutineDispatcher,
    private val apiKey: String,
    private val model: String,
    private val baseUrl: String = "https://api.anthropic.com/v1/"
) : AiApiClient {
    
    override val providerName = "anthropic"
    override val supportedModels = listOf("claude-3-5-sonnet-20241022", "claude-3-haiku-20240307")
    
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("x-api-key", apiKey)
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BaseUrlNormalizer.forAnthropic(baseUrl))
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
    }
    
    private val service = retrofit.create(AnthropicService::class.java)
    private val gson = Gson()
    
    override suspend fun generateQuestions(request: GenerateQuestionsRequest): Result<GenerateQuestionsResponse> = withContext(ioDispatcher) {
        try {
            val systemPrompt = buildSystemPrompt(request)
            val userPrompt = buildUserPrompt(request)
            
            val messages = listOf(AnthropicMessage("user", userPrompt))
            
            val req = AnthropicRequest(
                model = model,
                max_tokens = 4000,
                messages = messages,
                system = systemPrompt,
                temperature = 0.7
            )
            
            val response = service.createMessage(req)
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API Error: ${response.code()} - ${response.errorBody()?.string()}"))
            }
            
            val body = response.body() ?: return@withContext Result.failure(Exception("Empty response"))
            val content = body.content.firstOrNull()?.text ?: return@withContext Result.failure(Exception("No content"))
            
            val json = JsonParser.parseString(content).asJsonObject
            val questionsJson = json.getAsJsonArray("questions")
            
            val questions = questionsJson.asSequence()
                .map { it.asJsonObject }
                .map { parseGeneratedQuestion(it) }
                .filterNotNull()
                .toList()
            
            Result.success(GenerateQuestionsResponse(
                questions = questions,
                modelUsed = model,
                tokensUsed = (body.usage?.input_tokens ?: 0) + (body.usage?.output_tokens ?: 0)
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun analyzeLearning(request: AnalyzeLearningRequest): Result<AnalyzeLearningResponse> = withContext(ioDispatcher) {
        try {
            val prompt = buildAnalysisPrompt(request)
            
            val messages = listOf(AnthropicMessage("user", prompt))
            
            val req = AnthropicRequest(
                model = model,
                max_tokens = 2000,
                messages = messages,
                system = "あなたは学習分析の専門家です。ユーザーの学習履歴に基づいて、弱点分析、傾向分析、おすすめ学習計画をJSON形式で出力してください。",
                temperature = 0.5
            )
            
            val response = service.createMessage(req)
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API Error: ${response.code()}"))
            }
            
            val body = response.body() ?: return@withContext Result.failure(Exception("Empty response"))
            val content = body.content.firstOrNull()?.text ?: return@withContext Result.failure(Exception("No content"))
            
            Result.success(AnalyzeLearningResponse(
                analysis = content,
                modelUsed = model,
                tokensUsed = (body.usage?.input_tokens ?: 0) + (body.usage?.output_tokens ?: 0)
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun testConnection(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val messages = listOf(AnthropicMessage("user", "Hello"))
            val req = AnthropicRequest(model = model, max_tokens = 5, messages = messages)
            val response = service.createMessage(req)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("Test failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun listModels(): Result<List<String>> = withContext(ioDispatcher) {
        try {
            val response = service.listModels()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("モデル一覧の取得に失敗: ${response.code()} - ${response.errorBody()?.string().orEmpty()}")
                )
            }
            val body = response.body() ?: return@withContext Result.failure(Exception("Empty response"))
            val models = sanitizeChatModelIds(body.data.map { it.id })
            if (models.isEmpty()) {
                Result.failure(Exception("利用可能なモデルが見つかりませんでした"))
            } else {
                Result.success(models)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSystemPrompt(request: GenerateQuestionsRequest): String {
        return """
            あなたは優秀な問題作成AIです。以下の条件で4択問題を作成してください。
            
            教科: ${request.learningSet.subject}
            科目: ${request.learningSet.category}
            単元: ${request.learningSet.unit}
            学習範囲: ${request.learningSet.scope}
            
            条件:
            - 難易度: ${request.difficultyStart}〜${request.difficultyEnd} (1:易しい〜5:難しい)
            - 問題数: ${request.count}問
            - 重複回避: 既出問題と類似しないこと
            - 出力形式: JSONのみ
            
            JSON形式:
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
    
    private fun buildUserPrompt(request: GenerateQuestionsRequest): String {
        val previous = request.previousQuestions.joinToString("\n") { "- ${it.questionText} (${it.category}, 難易度${it.difficulty})" }
        val exclude = request.excludeQuestionIds.joinToString(", ")
        return """
            以下の条件で${request.count}問の4択問題を生成してください。
            
            ${if (previous.isNotBlank()) "既出問題（重複しないよう注意）:\n$previous\n" else ""}
            ${if (exclude.isNotBlank()) "除外ID: $exclude\n" else ""}
            ${if (request.categories.isNotEmpty()) "対象分野: ${request.categories.joinToString(", ")}\n" else ""}
            
            必ずJSON形式で出力してください。
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

interface AnthropicService {
    @retrofit2.http.POST("messages")
    @retrofit2.http.Headers(
        "anthropic-version: 2023-06-01",
        "content-type: application/json"
    )
    suspend fun createMessage(
        @retrofit2.http.Body request: AnthropicRequest
    ): retrofit2.Response<AnthropicResponse>

    @retrofit2.http.GET("models")
    @retrofit2.http.Headers("anthropic-version: 2023-06-01")
    suspend fun listModels(): retrofit2.Response<ModelListResponse>
}
