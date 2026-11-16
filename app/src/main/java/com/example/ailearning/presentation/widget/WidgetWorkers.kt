package com.example.ailearning.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ailearning.R
import com.example.ailearning.domain.model.Question
import com.example.ailearning.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetAnswerWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val processWidgetAnswerUseCase = AppContainer.processWidgetAnswerUseCase
    private val getWidgetQuestionUseCase = AppContainer.getWidgetQuestionUseCase

    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt("widget_id", 0)
        val selectedAnswer = inputData.getInt("selected_answer", -1)

        return withContext(Dispatchers.IO) {
            val questionResult = getWidgetQuestionUseCase(widgetId)
            if (questionResult.isFailure) return@withContext Result.failure()

            val question = questionResult.getOrNull() ?: return@withContext Result.failure()

            val result = processWidgetAnswerUseCase(widgetId, question.id, selectedAnswer, 1000)

            if (result.isFailure) return@withContext Result.failure()

            val answerResult = result.getOrNull()!!

            updateWidgetResult(context, widgetId, question, answerResult)

            Result.success()
        }
    }

    private fun updateWidgetResult(
        context: Context,
        widgetId: Int,
        question: Question,
        result: com.example.ailearning.domain.usecase.WidgetAnswerResult
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.widget_quiz)

        views.setViewVisibility(R.id.options_container, android.view.View.GONE)
        views.setViewVisibility(R.id.result_container, android.view.View.VISIBLE)

        val resultText = if (result.isCorrect) "正解！" else "不正解…"
        val resultColor = if (result.isCorrect) 0xFF2E7D32.toInt() else 0xFFC62828.toInt()
        views.setTextViewText(R.id.tv_result, resultText)
        views.setTextColor(R.id.tv_result, resultColor)
        views.setTextViewText(R.id.tv_explanation, result.explanation)

        val nextIntent = android.content.Intent(context, WidgetAnswerReceiver::class.java)
            .putExtra("widget_id", widgetId)
            .putExtra("action", "next")
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context, widgetId * 100,
            nextIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_next, pendingIntent)
        views.setViewVisibility(R.id.btn_next, android.view.View.VISIBLE)

        manager.updateAppWidget(widgetId, views)
    }
}

class WidgetNextWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val getWidgetQuestionUseCase = AppContainer.getWidgetQuestionUseCase

    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt("widget_id", 0)

        return withContext(Dispatchers.IO) {
            val questionResult = getWidgetQuestionUseCase(widgetId)
            if (questionResult.isFailure) return@withContext Result.failure()

            val question = questionResult.getOrNull() ?: return@withContext Result.failure()

            val manager = AppWidgetManager.getInstance(context)
            val views = RemoteViews(context.packageName, R.layout.widget_quiz)

            views.setTextViewText(R.id.tv_question, question.questionText)
            views.setTextViewText(R.id.btn_option_a, "A. ${question.optionA}")
            views.setTextViewText(R.id.btn_option_b, "B. ${question.optionB}")
            views.setTextViewText(R.id.btn_option_c, "C. ${question.optionC}")
            views.setTextViewText(R.id.btn_option_d, "D. ${question.optionD}")

            views.setViewVisibility(R.id.options_container, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.result_container, android.view.View.GONE)
            views.setViewVisibility(R.id.btn_next, android.view.View.GONE)

            for (i in 0..3) {
                val btnId = when (i) {
                    0 -> R.id.btn_option_a
                    1 -> R.id.btn_option_b
                    2 -> R.id.btn_option_c
                    else -> R.id.btn_option_d
                }
                val answerIntent = android.content.Intent(context, WidgetAnswerReceiver::class.java)
                    .putExtra("widget_id", widgetId)
                    .putExtra("action", "answer")
                    .putExtra("selected_answer", i)
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context, widgetId * 10 + i,
                    answerIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(btnId, pendingIntent)
            }

            manager.updateAppWidget(widgetId, views)

            Result.success()
        }
    }
}

class UpdateWidgetCacheWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val updateWidgetCacheUseCase = AppContainer.updateWidgetCacheUseCase

    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt("widget_id", 0)

        return withContext(Dispatchers.IO) {
            val result = updateWidgetCacheUseCase(widgetId)
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        }
    }
}
