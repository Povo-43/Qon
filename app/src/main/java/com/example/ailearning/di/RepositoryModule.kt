package com.example.ailearning.di

import com.example.ailearning.data.local.db.*
import com.example.ailearning.data.repository.*
import com.example.ailearning.domain.repository.*

object RepositoryModule {
    
    fun provideLearningSetRepository(dao: LearningSetDao): LearningSetRepository =
        LearningSetRepositoryImpl(dao)
    
    fun provideQuestionRepository(dao: QuestionDao): QuestionRepository =
        QuestionRepositoryImpl(dao)
    
    fun provideAnswerHistoryRepository(dao: AnswerHistoryDao): AnswerHistoryRepository =
        AnswerHistoryRepositoryImpl(dao)
    
    fun provideApiKeyRepository(dao: ApiKeyDao): ApiKeyRepository =
        ApiKeyRepositoryImpl(dao)
    
    fun provideLearningSessionRepository(dao: LearningSessionDao): LearningSessionRepository =
        LearningSessionRepositoryImpl(dao)
    
    fun provideAiAnalysisRepository(dao: AiAnalysisDao): AiAnalysisRepository =
        AiAnalysisRepositoryImpl(dao)
    
    fun provideWidgetConfigRepository(dao: WidgetConfigDao): WidgetConfigRepository =
        WidgetConfigRepositoryImpl(dao)

    fun provideFocusRepository(dao: FocusBlockedAppDao): FocusRepository =
        FocusRepositoryImpl(dao)
}
