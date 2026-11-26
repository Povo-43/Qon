package com.example.ailearning.di

import android.content.Context
import com.example.ailearning.data.local.db.AppDatabase
import com.example.ailearning.data.local.db.*
import com.example.ailearning.data.repository.*
import com.example.ailearning.domain.repository.*
import com.example.ailearning.domain.usecase.*
import com.example.ailearning.data.remote.api.*
import com.example.ailearning.data.local.encryption.EncryptionManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

object AppContainer {
    private lateinit var database: AppDatabase
    private lateinit var encryptionManager: EncryptionManager
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /** WorkManager などから使うアプリケーションコンテキスト。 */
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        database = DatabaseModule.provideDatabase(context)
        encryptionManager = EncryptionManager(context)
    }

    // DAOs
    val learningSetDao: LearningSetDao get() = database.learningSetDao()
    val questionDao: QuestionDao get() = database.questionDao()
    val answerHistoryDao: AnswerHistoryDao get() = database.answerHistoryDao()
    val apiKeyDao: ApiKeyDao get() = database.apiKeyDao()
    val learningSessionDao: LearningSessionDao get() = database.learningSessionDao()
    val aiAnalysisDao: AiAnalysisDao get() = database.aiAnalysisDao()
    val widgetConfigDao: WidgetConfigDao get() = database.widgetConfigDao()
    val focusBlockedAppDao: FocusBlockedAppDao get() = database.focusBlockedAppDao()

    // Repositories
    val learningSetRepository: LearningSetRepository get() = RepositoryModule.provideLearningSetRepository(learningSetDao)
    val questionRepository: QuestionRepository get() = RepositoryModule.provideQuestionRepository(questionDao)
    val answerHistoryRepository: AnswerHistoryRepository get() = RepositoryModule.provideAnswerHistoryRepository(answerHistoryDao)
    val apiKeyRepository: ApiKeyRepository get() = RepositoryModule.provideApiKeyRepository(apiKeyDao)
    val learningSessionRepository: LearningSessionRepository get() = RepositoryModule.provideLearningSessionRepository(learningSessionDao)
    val aiAnalysisRepository: AiAnalysisRepository get() = RepositoryModule.provideAiAnalysisRepository(aiAnalysisDao)
    val widgetConfigRepository: WidgetConfigRepository get() = RepositoryModule.provideWidgetConfigRepository(widgetConfigDao)
    val focusRepository: FocusRepository get() = RepositoryModule.provideFocusRepository(focusBlockedAppDao)

    // API clients
    val openAiClient: OpenAiApiClient get() = NetworkModule.provideOpenAiClient(ioDispatcher, encryptionManager)
    val anthropicClient: AnthropicApiClient get() = NetworkModule.provideAnthropicClient(ioDispatcher, encryptionManager)
    val geminiClient: GeminiApiClient get() = NetworkModule.provideGeminiClient(ioDispatcher, encryptionManager)

    // Client factory
    val aiClientFactory: AiClientFactory get() = NetworkModule.provideAiClientFactory(
        ioDispatcher, encryptionManager, openAiClient, anthropicClient, geminiClient
    )

    // AI Repository
    val aiRepository: com.example.ailearning.domain.repository.AiRepository get() = NetworkModule.provideAiRepository(
        ioDispatcher, apiKeyRepository, aiClientFactory
    )

    // Use cases - Question
    val generateQuestionsUseCase: GenerateQuestionsUseCase get() = GenerateQuestionsUseCase(aiRepository, questionRepository, learningSetRepository)
    val getRandomQuestionsUseCase: GetRandomQuestionsUseCase get() = GetRandomQuestionsUseCase(questionRepository)
    val getWeakQuestionsUseCase: GetWeakQuestionsUseCase get() = GetWeakQuestionsUseCase(questionRepository)
    val getQuestionsByCategoryUseCase: GetQuestionsByCategoryUseCase get() = GetQuestionsByCategoryUseCase(questionRepository)
    val getQuestionsByDifficultyUseCase: GetQuestionsByDifficultyUseCase get() = GetQuestionsByDifficultyUseCase(questionRepository)
    val getQuestionByIdUseCase: GetQuestionByIdUseCase get() = GetQuestionByIdUseCase(questionRepository)
    val checkDuplicateQuestionUseCase: CheckDuplicateQuestionUseCase get() = CheckDuplicateQuestionUseCase(questionRepository)

    // Use cases - Learning
    val startLearningSessionUseCase: StartLearningSessionUseCase get() = StartLearningSessionUseCase(learningSessionRepository)
    val recordAnswerUseCase: RecordAnswerUseCase get() = RecordAnswerUseCase(answerHistoryRepository, learningSessionRepository, questionRepository)
    val endSessionUseCase: EndLearningSessionUseCase get() = EndLearningSessionUseCase(learningSessionRepository)
    val getSessionHistoryUseCase: GetSessionHistoryUseCase get() = GetSessionHistoryUseCase(answerHistoryRepository)
    val getAnsweredQuestionIdsBySessionUseCase: GetAnsweredQuestionIdsBySessionUseCase get() = GetAnsweredQuestionIdsBySessionUseCase(answerHistoryRepository)
    val getAnsweredQuestionIdsByLearningSetUseCase: GetAnsweredQuestionIdsByLearningSetUseCase get() = GetAnsweredQuestionIdsByLearningSetUseCase(answerHistoryRepository)
    val getAnsweredQuestionIdsByWidgetIdUseCase: GetAnsweredQuestionIdsByWidgetIdUseCase get() = GetAnsweredQuestionIdsByWidgetIdUseCase(answerHistoryRepository)

    // Use cases - LearningSet
    val createLearningSetUseCase: CreateLearningSetUseCase get() = CreateLearningSetUseCase(learningSetRepository)
    val getLearningSetsUseCase: GetLearningSetsUseCase get() = GetLearningSetsUseCase(learningSetRepository)
    val getLearningSetUseCase: GetLearningSetUseCase get() = GetLearningSetUseCase(learningSetRepository)
    val updateLearningSetUseCase: UpdateLearningSetUseCase get() = UpdateLearningSetUseCase(learningSetRepository)
    val deleteLearningSetUseCase: DeleteLearningSetUseCase get() = DeleteLearningSetUseCase(learningSetRepository)

    // Use cases - Statistics
    val getOverallStatisticsUseCase: GetOverallStatisticsUseCase get() = GetOverallStatisticsUseCase(answerHistoryRepository, learningSessionRepository)
    val getCategoryStatisticsUseCase: GetCategoryStatisticsUseCase get() = GetCategoryStatisticsUseCase(answerHistoryRepository)
    val getDifficultyStatisticsUseCase: GetDifficultyStatisticsUseCase get() = GetDifficultyStatisticsUseCase(answerHistoryRepository)
    val getLearningTrendUseCase: GetLearningTrendUseCase get() = GetLearningTrendUseCase(answerHistoryRepository)

    // Use cases - Global Statistics (v1.6.6)
    val getGlobalStatisticsUseCase: GetGlobalStatisticsUseCase get() = GetGlobalStatisticsUseCase(answerHistoryRepository, settingsPreferences)
    val exportStatisticsUseCase: ExportStatisticsUseCase get() = ExportStatisticsUseCase(appContext, answerHistoryRepository)

    // Use cases - Category Export (v1.6.10)
    val exportStatisticsByCategoryUseCase: ExportStatisticsByCategoryUseCase get() = ExportStatisticsByCategoryUseCase(appContext, answerHistoryRepository)

    // Use cases - ApiKey
    val addApiKeyUseCase: AddApiKeyUseCase get() = AddApiKeyUseCase(apiKeyRepository, encryptionManager)
    val getAllApiKeysUseCase: GetAllApiKeysUseCase get() = GetAllApiKeysUseCase(apiKeyRepository)
    val getAvailableApiKeyUseCase: GetAvailableApiKeyUseCase get() = GetAvailableApiKeyUseCase(apiKeyRepository)
    val testApiKeyUseCase: TestApiKeyUseCase get() = TestApiKeyUseCase(ioDispatcher, apiKeyRepository, aiClientFactory)
    val fetchAvailableModelsUseCase: FetchAvailableModelsUseCase get() = FetchAvailableModelsUseCase(encryptionManager, aiClientFactory)
    val updateApiKeyUseCase: UpdateApiKeyUseCase get() = UpdateApiKeyUseCase(apiKeyRepository, encryptionManager)
    val deleteApiKeyUseCase: DeleteApiKeyUseCase get() = DeleteApiKeyUseCase(apiKeyRepository, encryptionManager)

    // Use cases - Analysis
    val analyzeLearningUseCase: AnalyzeLearningUseCase get() = AnalyzeLearningUseCase(aiRepository, aiAnalysisRepository, answerHistoryRepository, learningSetRepository, questionRepository)
    val getLatestAnalysisUseCase: GetLatestAnalysisUseCase get() = GetLatestAnalysisUseCase(aiAnalysisRepository)

    // Use cases - Widget
    val updateWidgetCacheUseCase: UpdateWidgetCacheUseCase get() = UpdateWidgetCacheUseCase(questionRepository, widgetConfigRepository)
    val getWidgetQuestionUseCase: GetWidgetQuestionUseCase get() = GetWidgetQuestionUseCase(questionRepository, widgetConfigRepository)
    val advanceWidgetQuestionUseCase: AdvanceWidgetQuestionUseCase get() = AdvanceWidgetQuestionUseCase(questionRepository, widgetConfigRepository)
    val processWidgetAnswerUseCase: ProcessWidgetAnswerUseCase get() = ProcessWidgetAnswerUseCase(answerHistoryRepository, questionRepository, widgetConfigRepository)
    val updateWidgetConfigUseCase: UpdateWidgetConfigUseCase get() = UpdateWidgetConfigUseCase(widgetConfigRepository)
    val createWidgetConfigUseCase: CreateWidgetConfigUseCase get() = CreateWidgetConfigUseCase(widgetConfigRepository)
    val deleteWidgetConfigUseCase: DeleteWidgetConfigUseCase get() = DeleteWidgetConfigUseCase(widgetConfigRepository)

    // Use cases - Focus (集中モード)
    val settingsPreferences: com.example.ailearning.data.local.prefs.SettingsPreferences by lazy {
        com.example.ailearning.data.local.prefs.SettingsPreferences(appContext)
    }
    val getFocusGateQuestionUseCase: GetFocusGateQuestionUseCase get() = GetFocusGateQuestionUseCase(questionRepository, learningSetRepository, settingsPreferences)
    val grantFocusUnlockUseCase: GrantFocusUnlockUseCase get() = GrantFocusUnlockUseCase(settingsPreferences)
    val recordFocusAnswerUseCase: RecordFocusAnswerUseCase get() = RecordFocusAnswerUseCase(answerHistoryRepository)
}
