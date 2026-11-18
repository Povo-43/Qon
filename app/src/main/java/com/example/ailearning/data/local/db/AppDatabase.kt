package com.example.ailearning.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.ailearning.domain.model.AiAnalysis
import com.example.ailearning.domain.model.ApiKey
import com.example.ailearning.domain.model.AnswerHistory
import com.example.ailearning.domain.model.FocusBlockedApp
import com.example.ailearning.domain.model.LearningSession
import com.example.ailearning.domain.model.LearningSet
import com.example.ailearning.domain.model.Question
import com.example.ailearning.domain.model.WidgetConfig

@Database(
    entities = [
        LearningSet::class,
        Question::class,
        AnswerHistory::class,
        ApiKey::class,
        LearningSession::class,
        AiAnalysis::class,
        WidgetConfig::class,
        FocusBlockedApp::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun learningSetDao(): LearningSetDao
    abstract fun questionDao(): QuestionDao
    abstract fun answerHistoryDao(): AnswerHistoryDao
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun learningSessionDao(): LearningSessionDao
    abstract fun aiAnalysisDao(): AiAnalysisDao
    abstract fun widgetConfigDao(): WidgetConfigDao
    abstract fun focusBlockedAppDao(): FocusBlockedAppDao
}
