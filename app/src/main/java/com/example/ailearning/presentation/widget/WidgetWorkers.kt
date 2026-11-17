package com.example.ailearning.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ailearning.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ウィジェットの再描画ワーカー。
 * 設定を読み、必要ならキャッシュを作り、現在の問題を描画する。
 */
class WidgetRefreshWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val widgetId = inputData.getInt("widget_id", 0)
        val manager = AppWidgetManager.getInstance(context)

        val config = AppContainer.widgetConfigRepository.getByWidgetId(widgetId).getOrNull()
        if (config == null) {
            WidgetRenderer.render(
                context, manager, widgetId,
                config = null,
                state = WidgetRenderer.WidgetState(
                    question = null,
                    emptyMessage = "設定を開いて\n学習セットを選択してください"
                )
            )
            return@withContext Result.success()
        }

        // キャッシュが空なら作成する
        if (config.getCachedQuestionIdList().isEmpty()) {
            AppContainer.updateWidgetCacheUseCase(widgetId)
        }

        val latestConfig = AppContainer.widgetConfigRepository.getByWidgetId(widgetId).getOrNull() ?: config
        val question = AppContainer.getWidgetQuestionUseCase(widgetId).getOrNull()

        // 定期更新スケジュールを確実に用意する（既にあれば KEEP で維持）
        WidgetScheduler.schedulePeriodicRefresh(context, widgetId, latestConfig.updateIntervalMinutes)

        if (question == null) {
            // 学習セットに問題が無い → 裏で事前生成を依頼して次回以降表示される
            com.example.ailearning.presentation.work.QuestionPrefetchScheduler.requestPrefetch(
                context, latestConfig.learningSetId
            )
        }

        WidgetRenderer.render(
            context, manager, widgetId,
            config = latestConfig,
            state = WidgetRenderer.WidgetState(
                question = question,
                questionNumber = if (question != null) latestConfig.currentIndex + 1 else 0
            )
        )
        Result.success()
    }
}

/** 選択肢タップ時の回答処理。 */
class WidgetAnswerWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val widgetId = inputData.getInt("widget_id", 0)
        val selectedAnswer = inputData.getInt("selected_answer", -1)
        if (selectedAnswer < 0) return@withContext Result.success()

        val manager = AppWidgetManager.getInstance(context)
        val question = AppContainer.getWidgetQuestionUseCase(widgetId).getOrNull()
            ?: return@withContext Result.failure()

        val result = AppContainer.processWidgetAnswerUseCase(widgetId, question.id, selectedAnswer, 1000)
        val answerResult = result.getOrNull() ?: return@withContext Result.failure()

        val config = AppContainer.widgetConfigRepository.getByWidgetId(widgetId).getOrNull()

        WidgetRenderer.render(
            context, manager, widgetId,
            config = config,
            state = WidgetRenderer.WidgetState(
                question = question,
                showResult = true,
                isCorrect = answerResult.isCorrect,
                selectedAnswer = selectedAnswer
            )
        )

        // 自動で次の問題へ（解説を少し見せるため遅延）
        if (config?.autoNext == true) {
            WidgetScheduler.requestNext(context, widgetId, delayMillis = 2500)
        }

        Result.success()
    }
}

/** 「次へ」でキャッシュ内の次の問題へ進む。 */
class WidgetNextWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val widgetId = inputData.getInt("widget_id", 0)
        val manager = AppWidgetManager.getInstance(context)

        val config = AppContainer.widgetConfigRepository.getByWidgetId(widgetId).getOrNull()
            ?: return@withContext Result.success()

        val question = AppContainer.advanceWidgetQuestionUseCase.next(widgetId)
            ?: return@withContext Result.success()

        WidgetRenderer.render(
            context, manager, widgetId,
            config = config,
            state = WidgetRenderer.WidgetState(
                question = question,
                questionNumber = (AppContainer.widgetConfigRepository.getByWidgetId(widgetId).getOrNull()
                    ?: config).currentIndex + 1
            )
        )
        Result.success()
    }
}

/** ウィジェット削除時に設定を掃除する。 */
class WidgetDeleteConfigWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val widgetId = inputData.getInt("widget_id", 0)
        WidgetScheduler.cancelPeriodicRefresh(context, widgetId)
        AppContainer.deleteWidgetConfigUseCase(widgetId)
        Result.success()
    }
}
