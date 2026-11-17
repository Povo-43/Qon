package com.example.ailearning.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

/**
 * Qon クイズウィジェット。
 *
 * 描画は [WidgetRenderer]、データ更新は WorkManager 経由の各 Worker が担当する。
 * onUpdate では DB 参照を行わず、即座にプレースホルダを描いてからリフレッシュを依頼する。
 */
class QuizWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            WidgetRenderer.renderPlaceholder(context, appWidgetManager, widgetId)
            WidgetScheduler.requestRefresh(context, widgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, widgetId, newOptions)
        // リサイズで背景ビットマップを作り直す
        WidgetRenderer.renderPlaceholder(context, appWidgetManager, widgetId)
        WidgetScheduler.requestRefresh(context, widgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            WidgetScheduler.requestDelete(context, widgetId)
        }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        // バックアップからの復元時は設定が残っていないためプレースホルダを描く
        newWidgetIds.forEach { widgetId ->
            WidgetRenderer.renderPlaceholder(context, AppWidgetManager.getInstance(context), widgetId)
            WidgetScheduler.requestRefresh(context, widgetId)
        }
    }
}
