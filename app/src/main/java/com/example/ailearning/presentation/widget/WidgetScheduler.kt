package com.example.ailearning.presentation.widget

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * ウィジェット操作の WorkManager 登録をまとめたクラス。
 * 同一ウィジェットへの連続リクエストは REPLACE で統合する。
 */
object WidgetScheduler {

    private fun workManager(context: Context): WorkManager =
        WorkManager.getInstance(context)

    fun requestRefresh(context: Context, widgetId: Int) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInputData(widgetData(widgetId))
            .build()
        workManager(context).enqueueUniqueWork("widget_refresh_$widgetId", ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * 設定した間隔でキャッシュを更新する定期ワーカー。
     * KEEP は既存スケジュールを維持する（都度リセットを避ける）。replace=true で間隔変更を反映。
     */
    fun schedulePeriodicRefresh(context: Context, widgetId: Int, intervalMinutes: Int, replace: Boolean = false) {
        val minutes = intervalMinutes.coerceAtLeast(15).toLong()
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(minutes, TimeUnit.MINUTES)
            .setInputData(widgetData(widgetId))
            .build()
        val policy = if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP
        workManager(context).enqueueUniquePeriodicWork("widget_periodic_$widgetId", policy, request)
    }

    fun cancelPeriodicRefresh(context: Context, widgetId: Int) {
        workManager(context).cancelUniqueWork("widget_periodic_$widgetId")
    }

    fun requestAnswer(context: Context, widgetId: Int, selectedAnswer: Int, questionId: String?) {
        val request = OneTimeWorkRequestBuilder<WidgetAnswerWorker>()
            .setInputData(
                Data.Builder()
                    .putInt("widget_id", widgetId)
                    .putInt("selected_answer", selectedAnswer)
                    .putString("question_id", questionId)
                    .build()
            )
            .build()
        workManager(context).enqueueUniqueWork("widget_answer_$widgetId", ExistingWorkPolicy.REPLACE, request)
    }

    fun requestNext(context: Context, widgetId: Int, delayMillis: Long = 0) {
        val builder = OneTimeWorkRequestBuilder<WidgetNextWorker>()
            .setInputData(widgetData(widgetId))
        if (delayMillis > 0) {
            builder.setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        }
        workManager(context).enqueueUniqueWork("widget_next_$widgetId", ExistingWorkPolicy.REPLACE, builder.build())
    }

    fun requestDelete(context: Context, widgetId: Int) {
        val request = OneTimeWorkRequestBuilder<WidgetDeleteConfigWorker>()
            .setInputData(widgetData(widgetId))
            .build()
        workManager(context).enqueueUniqueWork("widget_delete_$widgetId", ExistingWorkPolicy.REPLACE, request)
    }

    private fun widgetData(widgetId: Int): Data =
        Data.Builder().putInt("widget_id", widgetId).build()
}
