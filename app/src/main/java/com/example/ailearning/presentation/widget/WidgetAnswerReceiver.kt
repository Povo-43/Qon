package com.example.ailearning.presentation.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * ウィジェット操作（回答・次へ）を受け取るレシーバ。
 * 実処理は WorkManager のワーカーで行う。
 */
class WidgetAnswerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra("widget_id", 0)
        if (widgetId == 0) return

        when (intent.getStringExtra("action")) {
            "answer" -> {
                val selectedAnswer = intent.getIntExtra("selected_answer", -1)
                val questionId = intent.getStringExtra("question_id")
                WidgetScheduler.requestAnswer(context, widgetId, selectedAnswer, questionId)
            }
            "next" -> WidgetScheduler.requestNext(context, widgetId)
        }
    }
}
