package com.example.ailearning.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.ailearning.domain.model.Question
import com.example.ailearning.domain.model.WidgetConfig
import com.example.ailearning.R
import java.text.SimpleDateFormat
import java.util.Locale

class QuizWidgetProvider : AppWidgetProvider() {
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }
    
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, widgetId, newOptions)
        updateWidget(context, appWidgetManager, widgetId)
    }
    
    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        // この実装では、実際のデータ取得はWidgetConfigureActivityで行う
        // ここでは基本的なレイアウトのみ設定
        val views = RemoteViews(context.packageName, R.layout.widget_quiz)
        
        // クリックリスナー設定
        val pendingIntent = PendingIntent.getBroadcast(
            context, widgetId,
            Intent(context, WidgetAnswerReceiver::class.java)
                .putExtra("widget_id", widgetId)
                .putExtra("action", "next"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_next, pendingIntent)
        
        // オプションボタンのクリックリスナー
        for (i in 0..3) {
            val btnId = when (i) {
                0 -> R.id.btn_option_a
                1 -> R.id.btn_option_b
                2 -> R.id.btn_option_c
                else -> R.id.btn_option_d
            }
            val answerIntent = PendingIntent.getBroadcast(
                context, widgetId * 10 + i,
                Intent(context, WidgetAnswerReceiver::class.java)
                    .putExtra("widget_id", widgetId)
                    .putExtra("action", "answer")
                    .putExtra("selected_answer", i),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(btnId, answerIntent)
        }
        
        manager.updateAppWidget(widgetId, views)
    }
    
    companion object {
        fun updateWidgetContent(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            question: Question?,
            config: WidgetConfig?,
            showResult: Boolean = false,
            isCorrect: Boolean = false,
            selectedAnswer: Int = -1,
            explanation: String = ""
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_quiz)
            
            if (question != null) {
                views.setTextViewText(R.id.tv_question, question.questionText)
                views.setTextViewText(R.id.btn_option_a, "A. ${question.optionA}")
                views.setTextViewText(R.id.btn_option_b, "B. ${question.optionB}")
                views.setTextViewText(R.id.btn_option_c, "C. ${question.optionC}")
                views.setTextViewText(R.id.btn_option_d, "D. ${question.optionD}")
                
                views.setViewVisibility(R.id.options_container, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.result_container, android.view.View.GONE)
                views.setViewVisibility(R.id.btn_next, android.view.View.GONE)
            } else {
                views.setTextViewText(R.id.tv_question, context.getString(R.string.widget_no_questions))
                views.setViewVisibility(R.id.options_container, android.view.View.GONE)
                views.setViewVisibility(R.id.result_container, android.view.View.GONE)
                views.setViewVisibility(R.id.btn_next, android.view.View.GONE)
            }
            
            // 更新時刻
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeStr = config?.lastUpdatedAt?.let { timeFormat.format(java.util.Date(it)) } ?: "未更新"
            views.setTextViewText(R.id.tv_updated, context.getString(R.string.widget_last_updated, timeStr))
            
            manager.updateAppWidget(widgetId, views)
        }
        
        fun showResult(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            question: Question,
            isCorrect: Boolean,
            selectedAnswer: Int,
            explanation: String,
            config: WidgetConfig?
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_quiz)
            
            views.setViewVisibility(R.id.options_container, android.view.View.GONE)
            views.setViewVisibility(R.id.result_container, android.view.View.VISIBLE)
            
            val resultText = if (isCorrect) "正解！" else "不正解…"
            val resultColor = if (isCorrect) 0xFF2E7D32.toInt() else 0xFFC62828.toInt()
            views.setTextViewText(R.id.tv_result, resultText)
            views.setTextColor(R.id.tv_result, resultColor)
            views.setTextViewText(R.id.tv_explanation, explanation)
            
            if (config?.autoNext == true) {
                views.setViewVisibility(R.id.btn_next, android.view.View.GONE)
            } else {
                views.setViewVisibility(R.id.btn_next, android.view.View.VISIBLE)
            }
            
            manager.updateAppWidget(widgetId, views)
        }
    }
}
