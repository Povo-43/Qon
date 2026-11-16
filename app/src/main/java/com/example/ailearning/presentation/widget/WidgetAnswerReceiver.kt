package com.example.ailearning.presentation.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ailearning.R

class WidgetAnswerReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra("widget_id", 0)
        val action = intent.getStringExtra("action") ?: ""
        
        when (action) {
            "answer" -> {
                val selectedAnswer = intent.getIntExtra("selected_answer", -1)
                // 回答処理をワーカーで実行
                val work = OneTimeWorkRequestBuilder<WidgetAnswerWorker>()
                    .setInputData(androidx.work.Data.Builder()
                        .putInt("widget_id", widgetId)
                        .putInt("selected_answer", selectedAnswer)
                        .build())
                    .build()
                WorkManager.getInstance(context).enqueue(work)
            }
            "next" -> {
                // 次の問題へ
                val work = OneTimeWorkRequestBuilder<WidgetNextWorker>()
                    .setInputData(androidx.work.Data.Builder()
                        .putInt("widget_id", widgetId)
                        .build())
                    .build()
                WorkManager.getInstance(context).enqueue(work)
            }
        }
    }
}
