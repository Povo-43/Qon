package com.example.ailearning.data.remote.api

import com.example.ailearning.data.remote.model.OpenAiChatRequest
import com.example.ailearning.data.remote.model.OpenAiChatResponse
import com.example.ailearning.data.remote.model.OpenAiMessage
import com.example.ailearning.data.remote.model.OpenAiResponseFormat
import com.example.ailearning.data.remote.model.ModelListResponse
import com.example.ailearning.data.remote.model.sanitizeChatModelIds
import com.example.ailearning.domain.model.AnalyzeLearningRequest
import com.example.ailearning.domain.model.AnalyzeLearningResponse
import com.example.ailearning.domain.model.GenerateQuestionsRequest
import com.example.ailearning.domain.model.GenerateQuestionsResponse
import com.example.ailearning.domain.model.GeneratedQuestion
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/**
 * OpenAI 互換 `/chat/completions` クライアント。
 * OpenAI だけでなく OpenRouter / Groq / NVIDIA NIM / Kilo / Ollama などでも利用する。
 */
class OpenAiApiClient @Inject constructor(
    @Named("io_dispatcher") private val ioDispatcher: CoroutineDispatcher,
    private val apiKey: String,
    private val model: String,
    private val baseUrl: String = "https://api.openai.com/v1/",
    private val providerId: String = "openai"
) : AiApiClient {

    override val providerName = providerId
    override val supportedModels = listOf("gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo")

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                    .header("Accept", "application/json")

                if (apiKey.isNotBlank()) {
                    builder.header("Authorization", "Bearer $apiKey")
                }

                // OpenRouter は任意だが推奨ヘッダー
                if (providerId == "openrouter") {
                    builder.header("HTTP-Referer", "https://github.com/Povo-43/Qon")
                    builder.header("X-Title", "Qon")
                }

                chain.proceed(builder.build())
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BaseUrlNormalizer.forOpenAiCompatible(baseUrl))
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
    }

    private val service = retrofit.create(OpenAiService::class.java)

    private val chatEndpoint: String
        get() = BaseUrlNormalizer.forOpenAiCompatible(baseUrl) + "chat/completions"

    private val modelsEndpoint: String
        get() = BaseUrlNormalizer.forOpenAiCompatible(baseUrl) + "models"

    override suspend fun generateQuestions(request: GenerateQuestionsRequest): Result<GenerateQuestionsResponse> = withContext(ioDispatcher) {
        try {
            val systemPrompt = buildSystemPrompt(request)
            val userPrompt = buildUserPrompt(request)

            val messages = listOf(
                OpenAiMessage("system", systemPrompt),
                OpenAiMessage("user", userPrompt)
            )

            val req = OpenAiChatRequest(
                model = model,
                messages = messages,
                temperature = 0.7,
                response_format = OpenAiResponseFormat("json_object")
            )

            val response = chat(req)

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception(
                        ApiErrorDetails.http(
                            action = "問題生成",
                            provider = providerId,
                            model = model,
                            endpoint = chatEndpoint,
                            code = response.code(),
                            body = response.errorBody()?.string()
                        )
                    )
                )
            }

            val body = response.body() ?: return@withContext Result.failure(Exception("Empty response"))
            val content = body.choices.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("No content"))

            val json = extractJsonObject(content)
            val questionsJson = json.getAsJsonArray("questions")
                ?: return@withContext Result.failure(Exception("JSON に questions がありません"))

            val questions = questionsJson.asSequence()
                .map { it.asJsonObject }
                .map { parseGeneratedQuestion(it) }
                .filterNotNull()
                .toList()

            Result.success(
                GenerateQuestionsResponse(
                    questions = questions,
                    modelUsed = model,
                    tokensUsed = body.usage?.total_tokens ?: 0
                )
            )
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    ApiErrorDetails.exception("問題生成", providerId, model, chatEndpoint, e),
                    e
                )
            )
        }
    }

    override suspend fun analyzeLearning(request: AnalyzeLearningRequest): Result<AnalyzeLearningResponse> = withContext(ioDispatcher) {
        try {
            val prompt = buildAnalysisPrompt(request)

            val messages = listOf(
                OpenAiMessage("system", "あなたは学習分析の専門家です。ユーザーの学習履歴に基づいて、弱点分析、傾向分析、おすすめ学習計画をJSON形式で出力してください。"),
                OpenAiMessage("user", prompt)
            )

            val req = OpenAiChatRequest(
                model = model,
                messages = messages,
                temperature = 0.5,
                response_format = OpenAiResponseFormat("json_object")
            )

            val response = chat(req)

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception(
                        ApiErrorDetails.http(
                            action = "学習分析",
                            provider = providerId,
                            model = model,
                            endpoint = chatEndpoint,
                            code = response.code(),
                            body = response.errorBody()?.string()
                        )
                    )
                )
            }

            val body = response.body() ?: return@withContext Result.failure(Exception("Empty response"))
            val content = body.choices.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("No content"))

            Result.success(
                AnalyzeLearningResponse(
                    analysis = content,
                    modelUsed = model,
                    tokensUsed = body.usage?.total_tokens ?: 0
                )
            )
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    ApiErrorDetails.exception("学習分析", providerId, model, chatEndpoint, e),
                    e
                )
            )
        }
    }

    override suspend fun testConnection(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val messages = listOf(OpenAiMessage("user", "Hello"))
            val req = OpenAiChatRequest(model = model, messages = messages, max_tokens = 5)
            val response = service.chatCompletion(req)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception(
                        ApiErrorDetails.http(
                            action = "接続テスト",
                            provider = providerId,
                            model = model,
                            endpoint = chatEndpoint,
                            code = response.code(),
                            body = response.errorBody()?.string()
                        )
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    ApiErrorDetails.exception("接続テスト", providerId, model, chatEndpoint, e),
                    e
                )
            )
        }
    }

    override suspend fun listModels(): Result<List<String>> = withContext(ioDispatcher) {
        try {
            val response = service.listModels()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception(
                        ApiErrorDetails.http(
                            action = "モデル一覧の取得",
                            provider = providerId,
                            model = model,
                            endpoint = modelsEndpoint,
                            code = response.code(),
                            body = response.errorBody()?.string()
                        )
                    )
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
            Result.failure(
                Exception(
                    ApiErrorDetails.exception("モデル一覧の取得", providerId, model, modelsEndpoint, e),
                    e
                )
            )
        }
    }

    /**
     * response_format 非対応プロバイダ（NVIDIA NIM の一部、Perplexity 等）では
     * 自動的に response_format なしで再試行する。
     */
    private suspend fun chat(request: OpenAiChatRequest): Response<OpenAiChatResponse> {
        val first = service.chatCompletion(request)
        if (first.isSuccessful || request.response_format == null) return first

        val fallback = service.chatCompletion(request.copy(response_format = null))
        return if (fallback.isSuccessful) fallback else first
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
            - 出力形式: JSONのみ（前後に説明文やコードフェンスを付けない）
            
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
        val previous = request.previousQuestions.joinToString("\n") {
            "- ${it.questionText} (${it.category}, 難易度${it.difficulty})"
        }
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

    /** ```json フェンスや前後の説明文が付いていても JSON 部分だけを取り出す。 */
    private fun extractJsonObject(content: String): JsonObject {
        var text = content.trim()
        if (text.startsWith("```")) {
            text = text.removePrefix("```json")
                .removePrefix("```JSON")
                .removePrefix("```")
            val fenceEnd = text.lastIndexOf("```")
            if (fenceEnd >= 0) text = text.substring(0, fenceEnd)
            text = text.trim()
        }
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1)
        }
        return JsonParser.parseString(text).asJsonObject
    }

    private fun parseGeneratedQuestion(json: JsonObject): GeneratedQuestion? {
        return try {
            GeneratedQuestion(
                questionText = json.get("questionText").asString,
                options = json.get("options").asJsonArray.asSequence().map { it.asString }.toList(),
                correctAnswer = json.get("correctAnswer").asInt,
                explanation = json.get("explanation")?.asString.orEmpty(),
                difficulty = json.get("difficulty")?.asInt?.coerceIn(1, 5) ?: 3,
                category = json.get("category")?.asString.orEmpty()
            )
        } catch (e: Exception) {
            null
        }
    }
}

interface OpenAiService {
    @retrofit2.http.POST("chat/completions")
    suspend fun chatCompletion(@retrofit2.http.Body request: OpenAiChatRequest): Response<OpenAiChatResponse>

    @retrofit2.http.GET("models")
    suspend fun listModels(): Response<ModelListResponse>
}
