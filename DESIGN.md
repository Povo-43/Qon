# Android AI学習アプリ 設計書

## 1. プロジェクト概要

| 項目 | 内容 |
|------|------|
| アプリ名 | AI Quiz Learning (仮称) |
| パッケージ名 | com.example.ailearning |
| 対象OS | Android 8.0 (API 26) 以上 |
| 言語 | Kotlin 100% |
| アーキテクチャ | Clean Architecture + MVVM + Repository Pattern |
| DI | Hilt |
| 非同期処理 | Kotlin Coroutines + Flow |
| UI | Jetpack Compose (Material 3) |
| データベース | Room (SQLite) |
| 設定保存 | DataStore (Preferences) |
| 暗号化 | EncryptedSharedPreferences / SQLCipher |
| ネットワーク | Retrofit + OkHttp |
| 画像読み込み | Coil |
| テスト | JUnit4, MockK, Turbine, Compose UI Test, Robolectric |

---

## 2. アーキテクチャ設計

### 2.1 レイヤー構成

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Compose   │  │  ViewModel  │  │   AppWidgetProvider │  │
│  │   Screens   │  │   (State)   │  │   (RemoteViews)     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       Domain Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Use Cases  │  │  Entities   │  │  Repository Interfaces│  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        Data Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Repository  │  │   Room DB   │  │   API Clients       │  │
│  │ Impl        │  │   (DAO)     │  │   (Retrofit)        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ DataStore   │  │  WorkManager│  │  Encryption Utils   │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 モジュール構成

```
app/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/ailearning/
│   │   │   ├── AiLearningApplication.kt
│   │   │   ├── di/                    # Hilt modules
│   │   │   ├── presentation/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/         # Material3 Theme
│   │   │   │   │   ├── components/    # Shared Compose components
│   │   │   │   │   ├── home/          # Home screen
│   │   │   │   │   ├── learn/         # Learning screens
│   │   │   │   │   ├── manage/        # Learning set management
│   │   │   │   │   ├── settings/      # Settings screens
│   │   │   │   │   └── statistics/    # Statistics screens
│   │   │   │   ├── viewmodel/         # ViewModels
│   │   │   │   └── widget/            # AppWidgetProvider
│   │   │   ├── domain/
│   │   │   │   ├── model/             # Entities
│   │   │   │   ├── repository/        # Repository interfaces
│   │   │   │   └── usecase/           # Use cases
│   │   │   └── data/
│   │   │       ├── repository/        # Repository implementations
│   │   │       ├── local/
│   │   │       │   ├── db/            # Room database, DAOs
│   │   │       │   ├── prefs/         # DataStore
│   │   │       │   └── encryption/    # Encryption utilities
│   │   │       ├── remote/
│   │   │       │   ├── api/           # Retrofit APIs
│   │   │       │   └── model/         # API DTOs
│   │   │       └── mapper/            # DTO ↔ Entity mappers
│   │   └── res/
│   │       ├── values/                # Strings, colors, themes
│   │       ├── xml/                   # Widget configs, prefs
│   │       └── drawable/              # Icons, shapes
│   ├── test/                          # Unit tests
│   └── androidTest/                   # Instrumented tests
```

---

## 3. データモデル設計

### 3.1 Room エンティティ

```kotlin
// 学習セット
@Entity(tableName = "learning_sets")
data class LearningSet(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val subject: String,           // 教科
    val category: String,          // 科目
    val unit: String,              // 単元
    val scope: String,             // 学習範囲（自由記述）
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

// 問題
@Entity(tableName = "questions",
    indices = [Index("learningSetId"), Index("category"), Index("difficulty")])
data class Question(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val learningSetId: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: Int,        // 0-3 (A-D)
    val explanation: String,
    val difficulty: Int,           // 1-5
    val category: String,          // 分野タグ
    val source: String,            // "ai_generated" / "manual"
    val aiModelUsed: String?,      // 生成に使用したモデル
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    @Embedded val duplicateCheck: DuplicateCheck = DuplicateCheck()
)

@Embedded
data class DuplicateCheck(
    val contentHash: String = "",  // 問題文のハッシュ
    val optionsHash: String = "",  // 選択肢のハッシュ
    val similarityScore: Float = 0f
)

// 回答履歴
@Entity(tableName = "answer_histories",
    primaryKeys = ["id"],
    indices = [Index("questionId"), Index("learningSetId"), Index("answeredAt")])
data class AnswerHistory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val questionId: String,
    val learningSetId: String,
    val selectedAnswer: Int,       // -1: 未回答(スキップ), 0-3
    val isCorrect: Boolean,
    val responseTimeMs: Long,
    val answeredAt: Long = System.currentTimeMillis(),
    val sessionId: String,         // 学習セッションID
    val source: String             // "app" / "widget"
)

// APIキー管理
@Entity(tableName = "api_keys")
data class ApiKey(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,              // ユーザー定義名
    val provider: String,          // "openai" / "anthropic" / "google" / "custom"
    val model: String,             // モデル名
    val encryptedKey: String,      // 暗号化済みAPIキー
    val baseUrl: String?,          // カスタムエンドポイント用
    val priority: Int = 0,         // 優先度（高いほど優先）
    val isEnabled: Boolean = true,
    val usageType: String,         // "question_generation" / "analysis" / "both"
    val lastTestedAt: Long = 0,
    val lastTestSuccess: Boolean = false,
    val totalRequests: Long = 0,
    val totalTokensUsed: Long = 0,
    val errorCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// 学習セッション
@Entity(tableName = "learning_sessions")
data class LearningSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val learningSetId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long = 0,
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val totalTimeMs: Long = 0,
    val mode: String               // "continuous" / "review" / "random" / "unit"
)

// AI分析結果
@Entity(tableName = "ai_analyses")
data class AiAnalysis(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val learningSetId: String,
    val analysisType: String,      // "weakness" / "trend" / "recommendation"
    val content: String,           // JSON形式の分析結果
    val basedOnHistoryCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

// ウィジェット設定
@Entity(tableName = "widget_configs")
data class WidgetConfig(
    @PrimaryKey val widgetId: Int, // AppWidgetManagerのID
    val learningSetId: String,
    val updateIntervalMinutes: Int = 30,
    val questionCount: Int = 10,   // キャッシュする問題数
    val showExplanation: Boolean = true,
    val autoNext: Boolean = false,
    val lastUpdatedAt: Long = 0,
    val cachedQuestionIds: String = "" // JSON配列
)
```

### 3.2 DAO インターフェース

```kotlin
@Dao
interface LearningSetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: LearningSet): Long

    @Update
    suspend fun update(set: LearningSet): Int

    @Delete
    suspend fun delete(set: LearningSet): Int

    @Query("SELECT * FROM learning_sets WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getAllActive(): Flow<List<LearningSet>>

    @Query("SELECT * FROM learning_sets WHERE id = :id")
    fun getById(id: String): Flow<LearningSet?>

    @Query("SELECT * FROM learning_sets WHERE id = :id")
    suspend fun getByIdOnce(id: String): LearningSet?
}

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: Question): Long

    @Update
    suspend fun update(question: Question): Int

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND isActive = 1 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestions(learningSetId: String, limit: Int): List<Question>

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND isActive = 1 AND category = :category ORDER BY RANDOM() LIMIT :limit")
    suspend fun getQuestionsByCategory(learningSetId: String, category: String, limit: Int): List<Question>

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND isActive = 1 AND difficulty = :difficulty ORDER BY RANDOM() LIMIT :limit")
    suspend fun getQuestionsByDifficulty(learningSetId: String, difficulty: Int, limit: Int): List<Question>

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND isActive = 1 ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentQuestions(learningSetId: String, limit: Int): List<Question>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: String): Question?

    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND isActive = 1")
    fun getAllByLearningSet(learningSetId: String): Flow<List<Question>>

    @Query("SELECT COUNT(*) FROM questions WHERE learningSetId = :learningSetId AND isActive = 1")
    suspend fun countByLearningSet(learningSetId: String): Int

    // 重複チェック用
    @Query("SELECT * FROM questions WHERE learningSetId = :learningSetId AND contentHash = :hash AND isActive = 1")
    suspend fun findByContentHash(learningSetId: String, hash: String): Question?

    // 弱点問題取得（正答率が低い問題）
    @Query("""
        SELECT q.* FROM questions q
        JOIN (
            SELECT questionId, AVG(CASE WHEN isCorrect THEN 1.0 ELSE 0.0 END) as accuracy
            FROM answer_histories
            WHERE learningSetId = :learningSetId
            GROUP BY questionId
            HAVING COUNT(*) >= 3 AND accuracy < 0.6
        ) ah ON q.id = ah.questionId
        WHERE q.learningSetId = :learningSetId AND q.isActive = 1
        ORDER BY ah.accuracy ASC
        LIMIT :limit
    """)
    suspend fun getWeakQuestions(learningSetId: String, limit: Int): List<Question>
}

@Dao
interface AnswerHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: AnswerHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<AnswerHistory>): List<Long>

    @Query("SELECT * FROM answer_histories WHERE learningSetId = :learningSetId ORDER BY answeredAt DESC LIMIT :limit")
    suspend fun getRecentByLearningSet(learningSetId: String, limit: Int): List<AnswerHistory>

    @Query("SELECT * FROM answer_histories WHERE questionId = :questionId ORDER BY answeredAt DESC")
    suspend fun getByQuestionId(questionId: String): List<AnswerHistory>

    @Query("SELECT * FROM answer_histories WHERE sessionId = :sessionId ORDER BY answeredAt ASC")
    suspend fun getBySessionId(sessionId: String): List<AnswerHistory>

    @Query("SELECT * FROM answer_histories WHERE learningSetId = :learningSetId AND answeredAt >= :startTime AND answeredAt <= :endTime")
    suspend fun getByDateRange(learningSetId: String, startTime: Long, endTime: Long): List<AnswerHistory>

    @Query("SELECT COUNT(*) FROM answer_histories WHERE learningSetId = :learningSetId")
    suspend fun countByLearningSet(learningSetId: String): Int

    @Query("SELECT SUM(CASE WHEN isCorrect THEN 1 ELSE 0 END) * 1.0 / COUNT(*) FROM answer_histories WHERE learningSetId = :learningSetId")
    suspend fun getAccuracyByLearningSet(learningSetId: String): Double

    @Query("SELECT AVG(responseTimeMs) FROM answer_histories WHERE learningSetId = :learningSetId AND isCorrect = 1")
    suspend fun getAvgResponseTimeCorrect(learningSetId: String): Double

    // 単元別統計
    @Query("""
        SELECT q.category, 
               COUNT(ah.id) as total,
               SUM(CASE WHEN ah.isCorrect THEN 1 ELSE 0 END) as correct,
               AVG(ah.responseTimeMs) as avgTime
        FROM answer_histories ah
        JOIN questions q ON ah.questionId = q.id
        WHERE ah.learningSetId = :learningSetId AND ah.answeredAt >= :startTime
        GROUP BY q.category
    """)
    suspend fun getCategoryStats(learningSetId: String, startTime: Long): List<CategoryStat>

    // 難易度別統計
    @Query("""
        SELECT q.difficulty,
               COUNT(ah.id) as total,
               SUM(CASE WHEN ah.isCorrect THEN 1 ELSE 0 END) as correct,
               AVG(ah.responseTimeMs) as avgTime
        FROM answer_histories ah
        JOIN questions q ON ah.questionId = q.id
        WHERE ah.learningSetId = :learningSetId AND ah.answeredAt >= :startTime
        GROUP BY q.difficulty
        ORDER BY q.difficulty
    """)
    suspend fun getDifficultyStats(learningSetId: String, startTime: Long): List<DifficultyStat>
}

data class CategoryStat(
    val category: String,
    val total: Int,
    val correct: Int,
    val avgTime: Double
) {
    val accuracy: Double = if (total > 0) correct.toDouble() / total else 0.0
}

data class DifficultyStat(
    val difficulty: Int,
    val total: Int,
    val correct: Int,
    val avgTime: Double
) {
    val accuracy: Double = if (total > 0) correct.toDouble() / total else 0.0
}

@Dao
interface ApiKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: ApiKey): Long

    @Update
    suspend fun update(key: ApiKey): Int

    @Delete
    suspend fun delete(key: ApiKey): Int

    @Query("SELECT * FROM api_keys WHERE isEnabled = 1 ORDER BY priority DESC, createdAt ASC")
    fun getAllEnabled(): Flow<List<ApiKey>>

    @Query("SELECT * FROM api_keys WHERE isEnabled = 1 AND usageType IN (:usageType, 'both') ORDER BY priority DESC")
    suspend fun getByUsageType(usageType: String): List<ApiKey>

    @Query("SELECT * FROM api_keys WHERE id = :id")
    suspend fun getById(id: String): ApiKey?

    @Query("UPDATE api_keys SET totalRequests = totalRequests + 1, totalTokensUsed = totalTokensUsed + :tokens WHERE id = :id")
    suspend fun incrementUsage(id: String, tokens: Long)

    @Query("UPDATE api_keys SET errorCount = errorCount + 1, lastTestedAt = :time, lastTestSuccess = 0 WHERE id = :id")
    suspend fun recordError(id: String, time: Long)

    @Query("UPDATE api_keys SET lastTestedAt = :time, lastTestSuccess = 1, errorCount = 0 WHERE id = :id")
    suspend fun recordTestSuccess(id: String, time: Long)
}

@Dao
interface LearningSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: LearningSession): Long

    @Update
    suspend fun update(session: LearningSession): Int

    @Query("SELECT * FROM learning_sessions WHERE learningSetId = :learningSetId ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecentByLearningSet(learningSetId: String, limit: Int): List<LearningSession>

    @Query("SELECT * FROM learning_sessions WHERE id = :id")
    suspend fun getById(id: String): LearningSession?
}

@Dao
interface AiAnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: AiAnalysis): Long

    @Query("SELECT * FROM ai_analyses WHERE learningSetId = :learningSetId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentByLearningSet(learningSetId: String, limit: Int): List<AiAnalysis>

    @Query("SELECT * FROM ai_analyses WHERE learningSetId = :learningSetId AND analysisType = :type ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByType(learningSetId: String, type: String): AiAnalysis?
}

@Dao
interface WidgetConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: WidgetConfig): Long

    @Update
    suspend fun update(config: WidgetConfig): Int

    @Delete
    suspend fun delete(config: WidgetConfig): Int

    @Query("SELECT * FROM widget_configs WHERE widgetId = :widgetId")
    suspend fun getByWidgetId(widgetId: Int): WidgetConfig?

    @Query("SELECT * FROM widget_configs WHERE learningSetId = :learningSetId")
    suspend fun getByLearningSetId(learningSetId: String): List<WidgetConfig>

    @Query("SELECT * FROM widget_configs")
    fun getAll(): Flow<List<WidgetConfig>>
}
```

---

## 4. AI API 設計

### 4.1 対応プロバイダー

| プロバイダー | ベースURL | 主なモデル |
|-------------|-----------|-----------|
| OpenAI | https://api.openai.com/v1 | gpt-4o, gpt-4o-mini, gpt-3.5-turbo |
| Anthropic | https://api.anthropic.com/v1 | claude-3-5-sonnet, claude-3-haiku |
| Google (Gemini) | https://generativelanguage.googleapis.com/v1beta | gemini-1.5-pro, gemini-1.5-flash |
| Custom | ユーザー指定 | 任意 |

### 4.2 共通リクエスト/レスポンス形式

```kotlin
// 問題生成リクエスト
data class GenerateQuestionsRequest(
    val learningSet: LearningSet,
    val count: Int = 10,
    val difficultyRange: IntRange = 1..5,
    val categories: List<String> = emptyList(),
    val excludeQuestionIds: List<String> = emptyList(),
    val previousQuestions: List<QuestionSummary> = emptyList()
)

data class QuestionSummary(
    val questionText: String,
    val category: String,
    val difficulty: Int
)

// 問題生成レスポンス
data class GenerateQuestionsResponse(
    val questions: List<GeneratedQuestion>,
    val modelUsed: String,
    val tokensUsed: Int
)

data class GeneratedQuestion(
    val questionText: String,
    val options: List<String>,  // 4つ
    val correctAnswer: Int,     // 0-3
    val explanation: String,
    val difficulty: Int,        // 1-5
    val category: String
)

// 学習分析リクエスト
data class AnalyzeLearningRequest(
    val learningSet: LearningSet,
    val recentHistories: List<AnswerHistory>,
    val categoryStats: List<CategoryStat>,
    val difficultyStats: List<DifficultyStat>,
    val analysisType: String  // "weakness" / "trend" / "recommendation"
)

// 学習分析レスポンス
data class AnalyzeLearningResponse(
    val analysis: String,       // JSON形式
    val modelUsed: String,
    val tokensUsed: Int
)
```

### 4.3 プロバイダー別実装

```kotlin
interface AiApiClient {
    suspend fun generateQuestions(request: GenerateQuestionsRequest): Result<GenerateQuestionsResponse>
    suspend fun analyzeLearning(request: AnalyzeLearningRequest): Result<AnalyzeLearningResponse>
    suspend fun testConnection(): Result<Unit>
    val providerName: String
    val supportedModels: List<String>
}

// OpenAI実装
class OpenAiApiClient @Inject constructor(
    private val service: OpenAiService,
    private val apiKey: String
) : AiApiClient {
    override val providerName = "openai"
    override val supportedModels = listOf("gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo")

    override suspend fun generateQuestions(request: GenerateQuestionsRequest): Result<GenerateQuestionsResponse> {
        // 実装詳細後述
    }
    // ...
}
```

---

## 5. ユースケース設計

### 5.1 学習セット管理

```kotlin
// 学習セット作成
class CreateLearningSetUseCase @Inject constructor(
    private val repository: LearningSetRepository
) {
    operator fun invoke(name: String, subject: String, category: String, unit: String, scope: String): Result<LearningSet>
}

// 学習セット一覧取得
class GetLearningSetsUseCase @Inject constructor(
    private val repository: LearningSetRepository
) {
    operator fun invoke(): Flow<List<LearningSet>>
}

// 学習セット更新
class UpdateLearningSetUseCase @Inject constructor(
    private val repository: LearningSetRepository
) {
    operator fun invoke(set: LearningSet): Result<Unit>
}

// 学習セット削除
class DeleteLearningSetUseCase @Inject constructor(
    private val repository: LearningSetRepository
) {
    operator fun invoke(id: String): Result<Unit>
}
```

### 5.2 問題生成・管理

```kotlin
// AI問題生成
class GenerateQuestionsUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val questionRepository: QuestionRepository,
    private val learningSetRepository: LearningSetRepository
) {
    operator fun invoke(learningSetId: String, count: Int = 10): Result<List<Question>>
}

// 問題取得（ランダム）
class GetRandomQuestionsUseCase @Inject constructor(
    private val repository: QuestionRepository
) {
    operator fun invoke(learningSetId: String, limit: Int): Result<List<Question>>
}

// 弱点問題取得
class GetWeakQuestionsUseCase @Inject constructor(
    private val repository: QuestionRepository
) {
    operator fun invoke(learningSetId: String, limit: Int): Result<List<Question>>
}

// 単元別問題取得
class GetQuestionsByCategoryUseCase @Inject constructor(
    private val repository: QuestionRepository
) {
    operator fun invoke(learningSetId: String, category: String, limit: Int): Result<List<Question>>
}

// 重複チェック
class CheckDuplicateQuestionUseCase @Inject constructor(
    private val repository: QuestionRepository
) {
    operator fun invoke(learningSetId: String, question: Question): Boolean
}
```

### 5.3 学習実行

```kotlin
// 学習セッション開始
class StartLearningSessionUseCase @Inject constructor(
    private val sessionRepository: LearningSessionRepository
) {
    operator fun invoke(learningSetId: String, mode: String): Result<LearningSession>
}

// 回答記録
class RecordAnswerUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository,
    private val sessionRepository: LearningSessionRepository,
    private val questionRepository: QuestionRepository
) {
    operator fun invoke(
        sessionId: String,
        questionId: String,
        learningSetId: String,
        selectedAnswer: Int,
        responseTimeMs: Long,
        source: String
    ): Result<AnswerHistory>
}

// 学習セッション終了
class EndLearningSessionUseCase @Inject constructor(
    private val sessionRepository: LearningSessionRepository
) {
    operator fun invoke(sessionId: String): Result<Unit>
}
```

### 5.4 AI分析

```kotlin
// 学習分析実行
class AnalyzeLearningUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val analysisRepository: AiAnalysisRepository,
    private val historyRepository: AnswerHistoryRepository,
    private val questionRepository: QuestionRepository
) {
    operator fun invoke(learningSetId: String, type: String): Result<AiAnalysis>
}

// 分析結果取得
class GetLatestAnalysisUseCase @Inject constructor(
    private val repository: AiAnalysisRepository
) {
    operator fun invoke(learningSetId: String, type: String): Result<AiAnalysis?>
}
```

### 5.5 APIキー管理

```kotlin
// APIキー追加
class AddApiKeyUseCase @Inject constructor(
    private val repository: ApiKeyRepository,
    private val encryption: EncryptionManager
) {
    operator fun invoke(
        name: String,
        provider: String,
        model: String,
        rawKey: String,
        baseUrl: String?,
        priority: Int,
        usageType: String
    ): Result<ApiKey>
}

// APIキー取得（優先度順、フォールバック対応）
class GetAvailableApiKeyUseCase @Inject constructor(
    private val repository: ApiKeyRepository
) {
    operator fun invoke(usageType: String): Result<ApiKey>
}

// APIキー疎通テスト
class TestApiKeyUseCase @Inject constructor(
    private val repository: ApiKeyRepository,
    private val aiClientFactory: AiClientFactory
) {
    operator fun invoke(keyId: String): Result<Boolean>
}

// APIキー更新（使用量記録）
class UpdateApiKeyUsageUseCase @Inject constructor(
    private val repository: ApiKeyRepository
) {
    operator fun invoke(keyId: String, tokens: Long): Result<Unit>
}
```

### 5.6 統計・分析

```kotlin
// 総合統計取得
class GetOverallStatisticsUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository,
    private val sessionRepository: LearningSessionRepository
) {
    operator fun invoke(learningSetId: String, period: StatisticsPeriod): Result<OverallStatistics>
}

// 単元別統計取得
class GetCategoryStatisticsUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository
) {
    operator fun invoke(learningSetId: String, period: StatisticsPeriod): Result<List<CategoryStatistics>>
}

// 難易度別統計取得
class GetDifficultyStatisticsUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository
) {
    operator fun invoke(learningSetId: String, period: StatisticsPeriod): Result<List<DifficultyStatistics>>
}

// 学習推移取得
class GetLearningTrendUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository
) {
    operator fun invoke(learningSetId: String, days: Int): Result<List<DailyStatistics>>
}

data class OverallStatistics(
    val totalQuestions: Int,
    val totalCorrect: Int,
    val overallAccuracy: Double,
    val totalStudyTimeMs: Long,
    val avgResponseTimeMs: Double,
    val totalSessions: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int
)

data class CategoryStatistics(
    val category: String,
    val total: Int,
    val correct: Int,
    val accuracy: Double,
    val avgResponseTimeMs: Double
)

data class DifficultyStatistics(
    val difficulty: Int,
    val total: Int,
    val correct: Int,
    val accuracy: Double,
    val avgResponseTimeMs: Double
)

data class DailyStatistics(
    val date: String,  // YYYY-MM-DD
    val total: Int,
    val correct: Int,
    val accuracy: Double,
    val studyTimeMs: Long
)

enum class StatisticsPeriod {
    TODAY, WEEK, MONTH, ALL
}
```

### 5.7 ウィジェット

```kotlin
// ウィジェット用問題キャッシュ更新
class UpdateWidgetCacheUseCase @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val widgetConfigRepository: WidgetConfigRepository
) {
    operator fun invoke(widgetId: Int): Result<Unit>
}

// ウィジェット回答処理
class ProcessWidgetAnswerUseCase @Inject constructor(
    private val historyRepository: AnswerHistoryRepository,
    private val questionRepository: QuestionRepository,
    private val widgetConfigRepository: WidgetConfigRepository
) {
    operator fun invoke(
        widgetId: Int,
        questionId: String,
        selectedAnswer: Int,
        responseTimeMs: Long
    ): Result<WidgetAnswerResult>
}

data class WidgetAnswerResult(
    val isCorrect: Boolean,
    val correctAnswer: Int,
    val explanation: String,
    val nextQuestion: Question?
)

// ウィジェット設定更新
class UpdateWidgetConfigUseCase @Inject constructor(
    private val repository: WidgetConfigRepository
) {
    operator fun invoke(config: WidgetConfig): Result<Unit>
}
```

---

## 6. UI設計

### 6.1 画面構成

```
┌──────────────────────────────────────────────────────────────┐
│                      Navigation Graph                         │
├──────────────────────────────────────────────────────────────┤
│  HomeScreen (Bottom Navigation)                              │
│  ├── LearningSetListScreen                                   │
│  │   └── LearningSetDetailScreen                             │
│  │       ├── CreateLearningSetScreen                         │
│  │       └── EditLearningSetScreen                          │
│  ├── LearnScreen (メイン学習画面)                              │
│  │   ├── ContinuousLearningScreen (連続学習)                  │
│  │   ├── ReviewWeakQuestionsScreen (弱点復習)                │
│  │   ├── RandomQuestionsScreen (ランダム出題)                │
│  │   └── UnitQuestionsScreen (単元別出題)                    │
│  │       └── QuestionScreen (問題表示・回答)                  │
│  │           └── ResultScreen (結果・解説表示)               │
│  ├── StatisticsScreen                                        │
│  │   ├── OverallStatisticsScreen                             │
│  │   ├── CategoryStatisticsScreen                            │
│  │   ├── DifficultyStatisticsScreen                          │
│  │   └── TrendScreen                                         │
│  │       └── AiReportScreen (AI分析レポート)                  │
│  └── SettingsScreen                                          │
│      ├── ApiKeyManagementScreen                              │
│      │   ├── AddApiKeyScreen                                 │
│      │   └── EditApiKeyScreen                                │
│      ├── WidgetSettingsScreen                                │
│      ├── DataManagementScreen (バックアップ/リストア)          │
│      └── AboutScreen                                         │
└──────────────────────────────────────────────────────────────┘
```

### 6.2 共通UIコンポーネント

```kotlin
// 共通コンポーネント
@Composable fun QuizOptionButton(
    text: String,
    label: Char,  // 'A'..'D'
    state: OptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)

enum class OptionState {
    DEFAULT, SELECTED, CORRECT, INCORRECT, DISABLED
}

@Composable fun ProgressIndicator(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
)

@Composable fun StatCard(
    title: String,
    value: String,
    subtitle: String?,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
)

@Composable fun EmptyState(
    illustration: ImageVector,
    title: String,
    message: String,
    actionText: String?,
    onAction: (() -> Unit)? = null
)

@Composable fun LoadingOverlay(
    isVisible: Boolean,
    message: String = "処理中..."
)
```

### 6.3 テーマ設計 (Material 3)

```kotlin
// Color.kt
val LightColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue900,
    secondary = Purple600,
    onSecondary = White,
    secondaryContainer = Purple100,
    onSecondaryContainer = Purple900,
    tertiary = Teal600,
    onTertiary = White,
    tertiaryContainer = Teal100,
    onTertiaryContainer = Teal900,
    error = Red600,
    onError = White,
    errorContainer = Red100,
    onErrorContainer = Red900,
    background = Gray50,
    onBackground = Gray900,
    surface = White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    outline = Gray400,
    outlineVariant = Gray300,
    inverseSurface = Gray900,
    inverseOnSurface = Gray50,
    inversePrimary = Blue200,
    shadow = Black,
    scrim = Black,
    surfaceTint = Blue600
)

val DarkColorScheme = darkColorScheme(
    primary = Blue300,
    onPrimary = Blue950,
    primaryContainer = Blue800,
    onPrimaryContainer = Blue100,
    secondary = Purple300,
    onSecondary = Purple950,
    secondaryContainer = Purple800,
    onSecondaryContainer = Purple100,
    tertiary = Teal300,
    onTertiary = Teal950,
    tertiaryContainer = Teal800,
    onTertiaryContainer = Teal100,
    error = Red300,
    onError = Red950,
    errorContainer = Red800,
    onErrorContainer = Red100,
    background = Gray950,
    onBackground = Gray50,
    surface = Gray900,
    onSurface = Gray50,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray300,
    outline = Gray600,
    outlineVariant = Gray700,
    inverseSurface = Gray50,
    inverseOnSurface = Gray950,
    inversePrimary = Blue700,
    shadow = Black,
    scrim = Black,
    surfaceTint = Blue300
)
```

---

## 7. セキュリティ設計

### 7.1 APIキー暗号化

```kotlin
class EncryptionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_api_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun encrypt(plainText: String): String {
        // AES-GCM で暗号化、Base64エンコード
    }

    fun decrypt(encryptedText: String): String {
        // Base64デコード、AES-GCM で復号
    }
}
```

### 7.2 Room暗号化 (SQLCipher)

```kotlin
// RoomDatabaseBuilderでSQLCipherを使用
Room.databaseBuilder(context, AppDatabase::class.java, "ai_learning.db")
    .openHelperFactory(SQLCipherOpenHelperFactory(encryptionManager.getDatabaseKey()))
    .build()
```

### 7.3 ログ出力制御

```kotlin
object SecureLogger {
    private const val TAG = "AILearning"
    
    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, sanitize(message))
    }
    
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, sanitize(message), throwable)
    }
    
    private fun sanitize(message: String): String {
        // APIキー、トークン、個人情報をマスク
        return message
            .replace(Regex("sk-[a-zA-Z0-9]{48}"), "sk-***")
            .replace(Regex("Bearer [a-zA-Z0-9\\-._~+/]+=*"), "Bearer ***")
            .replace(Regex("\"key\"\\s*:\\s*\"[^\"]+\""), "\"key\": \"***\"")
    }
}
```

---

## 8. オフライン対応・同期設計

### 8.1 WorkManager によるバックグラウンド処理

```kotlin
// 問題生成ワーカー
class GenerateQuestionsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val generateQuestionsUseCase: GenerateQuestionsUseCase,
    private val questionRepository: QuestionRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val learningSetId = inputData.getString("learning_set_id")!!
        val count = inputData.getInt("count", 10)
        return generateQuestionsUseCase(learningSetId, count)
            .fold(
                { Result.success() },
                { Result.retry() }
            )
    }
}

// ウィジェットキャッシュ更新ワーカー
class UpdateWidgetCacheWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updateWidgetCacheUseCase: UpdateWidgetCacheUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt("widget_id", 0)
        return updateWidgetCacheUseCase(widgetId)
            .fold({ Result.success() }, { Result.retry() })
    }
}

// 定期的な学習分析ワーカー
class AnalyzeLearningWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val analyzeLearningUseCase: AnalyzeLearningUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val learningSetId = inputData.getString("learning_set_id")!!
        analyzeLearningUseCase(learningSetId, "weakness")
        analyzeLearningUseCase(learningSetId, "trend")
        return Result.success()
    }
}
```

### 8.2 ウィジェット実装

```kotlin
class QuizWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val config = getWidgetConfig(widgetId)
        val question = getNextCachedQuestion(widgetId)
        
        val views = RemoteViews(context.packageName, R.layout.widget_quiz)
            .apply {
                if (question != null) {
                    setTextViewText(R.id.tv_question, question.questionText)
                    setTextViewText(R.id.tv_option_a, "A. ${question.optionA}")
                    setTextViewText(R.id.tv_option_b, "B. ${question.optionB}")
                    setTextViewText(R.id.tv_option_c, "C. ${question.optionC}")
                    setTextViewText(R.id.tv_option_d, "D. ${question.optionD}")
                    
                    // クリックリスナー設定
                    val pendingIntent = PendingIntent.getBroadcast(
                        context, widgetId,
                        Intent(context, WidgetAnswerReceiver::class.java)
                            .putExtra("widget_id", widgetId)
                            .putExtra("question_id", question.id)
                            .putExtra("selected_answer", 0), // 動的に設定
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setOnClickPendingIntent(R.id.btn_option_a, pendingIntent)
                    // ... B, C, D も同様
                } else {
                    setTextViewText(R.id.tv_question, "問題がありません\nアプリで問題を生成してください")
                }
                setTextViewText(R.id.tv_updated, "更新: ${formatTime(config.lastUpdatedAt)}")
            }
        
        manager.updateAppWidget(widgetId, views)
    }
}

class WidgetAnswerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // 回答処理、正誤表示、次の問題へ
    }
}
```

---

## 9. 実装計画・マイルストーン

### Phase 1: 基盤構築 (Week 1-2) - MVP Core
- [ ] プロジェクトセットアップ (Gradle, Hilt, Room, Compose, Navigation)
- [ ] データベーススキーマ作成・マイグレーション
- [ ] 基本エンティティ・DAO・Repository実装
- [ ] 暗号化ユーティリティ実装
- [ ] 基本テーマ・共通UIコンポーネント
- [ ] 学習セット CRUD (作成・一覧・編集・削除)
- [ ] 単体テスト: Repository, UseCase, Encryption

### Phase 2: 学習機能・AI連携 (Week 3-4) - MVP Learning
- [ ] AI APIクライアント実装 (OpenAI, Anthropic, Gemini)
- [ ] APIキー管理画面・機能 (追加・編集・テスト・優先度・フォールバック)
- [ ] 問題生成ユースケース・画面実装
- [ ] 連続学習モード実装 (問題表示→回答→結果→次の問題)
- [ ] 回答履歴保存・セッション管理
- [ ] 重複チェック機能
- [ ] 単体テスト: AI Client, Question Generation, Learning Flow

### Phase 3: 学習モード拡張・統計 (Week 5-6)
- [ ] 弱点復習モード
- [ ] ランダム出題モード
- [ ] 単元別出題モード
- [ ] 統計画面 (総合・単元別・難易度別・推移)
- [ ] グラフ表示 (MPAndroidChart または Compose Charts)
- [ ] AI学習分析実装 (弱点分析・傾向分析・レポート生成)
- [ ] 単体テスト: Statistics, Analysis

### Phase 4: ウィジェット・オフライン対応 (Week 7-8)
- [ ] AppWidgetProvider 実装
- [ ] RemoteViews レイアウト・操作
- [ ] ウィジェット設定画面
- [ ] 問題キャッシュ・プリフェッチ (WorkManager)
- [ ] ウィジェット回答処理・履歴同期
- [ ] 複数ウィジェット・複数学習セット対応
- [ ] オフライン時の動作確認
- [ ] 単体テスト: Widget, Cache, Sync

### Phase 5: 仕上げ・リリース準備 (Week 9-10)
- [ ] 設定画面完成 (データバックアップ/リストア、テーマ、通知等)
- [ ] UI/UX 改善・アクセシビリティ対応
- [ ] パフォーマンス最適化 (DB インデックス、ページング、キャッシュ)
- [ ] 統合テスト・UIテスト
- [ ] 実機テスト (複数APIキー、ネットワーク断、バッテリー最適化)
- [ ] Play Console 登録用アセット準備
- [ ] リリースビルド・署名設定

---

## 10. テスト戦略

### 10.1 テストピラミッド

```
           ┌─────────────┐
           │  E2E/UI     │  ← 10% (Instrumented: Compose UI Test, Robot Pattern)
           ├─────────────┤
           │ Integration │  ← 20% (Repository, UseCase, ViewModel with Turbine)
           ├─────────────┤
           │   Unit      │  ← 70% (Domain, Data mappers, Utils, ViewModel logic)
           └─────────────┘
```

### 10.2 主要テストケース

| 層 | 対象 | ツール | 重点項目 |
|----|------|--------|----------|
| Unit | UseCase, Mapper, Encryption, Validator | JUnit4, MockK, Turbine | 正常系・異常系・境界値 |
| Unit | ViewModel (StateFlow) | Turbine, MockK | 状態遷移・副作用・ライフサイクル |
| Integration | Repository (Room) | Room Testing, JUnit4 | CRUD・クエリ・トランザクション |
| Integration | AI Client (MockWebServer) | MockWebServer, JUnit4 | リクエスト/レスポンス・エラー・リトライ |
| UI | Screen, Widget | Compose Test, Robolectric | ユーザー操作・状態表示・ナビゲーション |
| E2E | 主要フロー | Compose Test, Espresso | 学習セット作成→問題生成→学習→統計確認 |

---

## 11. 依存ライブラリ (build.gradle.kts 抜粋)

```kotlin
// Versions
val composeBom = platform("androidx.compose:compose-bom:2024.08.00")
val hiltVersion = "2.51"
val roomVersion = "2.6.1"
val lifecycleVersion = "2.8.0"
val navigationVersion = "2.8.0"
val workVersion = "2.9.0"
val datastoreVersion = "1.1.1"
val retrofitVersion = "2.11.0"
val okhttpVersion = "4.12.0"
val coilVersion = "2.7.0"
val mockkVersion = "1.13.13"
val turbineVersion = "1.0.1"
val junitVersion = "4.13.2"
val robolectricVersion = "4.13"
val truthVersion = "1.7.1"

// Dependencies
dependencies {
    // Compose
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.navigation:navigation-compose:$navigationVersion")

    // Hilt
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    // SQLCipher
    implementation("net.zetetic:android-database-sqlcipher:4.5.7")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:$datastoreVersion")
    implementation("androidx.datastore:datastore-core:$datastoreVersion")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:$workVersion")
    implementation("androidx.hilt:hilt-work:$hiltVersion")

    // Network
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:$retrofitVersion")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Coil
    implementation("io.coil-kt:coil-compose:$coilVersion")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // or implementation("androidx.compose.charts:charts:1.0.0-alpha01")

    // Test
    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("app.cash.turbine:turbine:$turbineVersion")
    testImplementation("org.robolectric:robolectric:$robolectricVersion")
    testImplementation("com.google.truth:truth:$truthVersion")
    
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

---

## 12. リスクと対策

| リスク | 影響度 | 発生確率 | 対策 |
|--------|--------|----------|------|
| AI API仕様変更・制限 | 高 | 中 | 抽象化層でプロバイダー切り替え可能に、モック対応 |
| APIキー漏洩 | 高 | 低 | 暗号化保存、ログマスク、ProGuard/R8難読化 |
| ウィジェット更新制限(Android) | 中 | 高 | WorkManagerで定期更新、キャッシュ活用、ユーザー手動更新も可能に |
| Room/SQLCipher互換性 | 中 | 低 | バージョン固定、マイグレーションテスト徹底 |
| 大量データでのDB性能 | 中 | 中 | インデックス最適化、ページング、非同期クエリ |
| オフライン時のデータ整合性 | 中 | 中 | ローカルファースト、競合解決戦略(最終書き込み優先) |
| ストア審査リジェクト | 高 | 低 | ポリシー遵守、プライバシーポリシー、ターゲットAPI準拠 |

---

## 13. 今後の拡張余地

1. **クラウド同期**: Firebase/AppSync でデバイス間同期
2. **コミュニティ機能**: 学習セット共有、ランキング
3. **音声対応**: TTS/STT で音声学習
4. **マルチモーダル**: 画像問題生成 (図表・写真)
5. **適応学習**: IRT(項目反応理論)ベースの難易度調整
6. **学習計画**: 目標設定・スケジュール・リマインド
7. **エクスポート**: Anki/CSV/PDF 出力

---

*作成日: 2025年*
*バージョン: 1.0*