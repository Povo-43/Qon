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
        // タップされたボタンには「その時表示されていた問題のID」が入っている。
        // これを使って判定することで、処理の間に表示対象が変わっても
        // 押した問題そのもので正誤判定が行われる。
        val tappedQuestionId = inputData.getString("question_id")
        if (selectedAnswer < 0) return@withContext Result.success()

        val manager = AppWidgetManager.getInstance(context)

        // 表示されていた問題を最優先で使う。取れない場合のみ現在位置にフォールバック
        val question = tappedQuestionId?.let { id ->
            AppContainer.questionRepository.getById(id).getOrNull()
        } ?: AppContainer.getWidgetQuestionUseCase(widgetId).getOrNull()
            ?: return@withContext Result.failure()

        // 結果表示中に残ってしまった古いタップ（RemoteViews の再適用で
        // クリックが残る環境がある）で同じ問題に二重記録されるのを防ぐ
        if (!WidgetAnswerState.canRecord(context, widgetId, question.id)) {
            val config = AppContainer.widgetConfigRepository.getByWidgetId(widgetId).getOrNull()
            WidgetRenderer.render(
                context, manager, widgetId,
                config = config,
                state = WidgetRenderer.WidgetState(question = question)
            )
            return@withContext Result.success()
        }
        WidgetAnswerState.markRecorded(context, widgetId, question.id)

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

/**
 * 同じ問題への二重記録を防ぐための「最後に記録した問題」の記録。
 * ウィジェットは RemoteViews の再適用で結果表示中も古いクリックが
 * 残ることがあり、そのタップで誤った不正解が記録されるのを防ぐ。
 */
object WidgetAnswerState {

    private const val PREFS_NAME = "widget_answer_state"
    private const val KEY_PREFIX = "answered_"

    /** windowMs 以内に同じ問題が記録されていなければ true */
    fun canRecord(context: Context, widgetId: Int, questionId: String, now: Long = System.currentTimeMillis()): Boolean {
        val raw = prefs(context).getString(key(widgetId), null) ?: return true
        val (lastId, lastAt) = parse(raw) ?: return true
        return !(lastId == questionId && now - lastAt < DUPLICATE_WINDOW_MS)
    }

    fun markRecorded(context: Context, widgetId: Int, questionId: String, now: Long = System.currentTimeMillis()) {
        prefs(context).edit().putString(key(widgetId), "$questionId|$now").apply()
    }

    private fun parse(raw: String): Pair<String, Long>? {
        val index = raw.lastIndexOf('|')
        if (index <= 0) return null
        val at = raw.substring(index + 1).toLongOrNull() ?: return null
        return raw.substring(0, index) to at
    }

    private fun key(widgetId: Int) = "$KEY_PREFIX$widgetId"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private const val DUPLICATE_WINDOW_MS = 10_000L
}

/** 「次へ」でキャッシュ内の次の問題へ進む。 */
class WidgetNextWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val widgetId = inputData.getInt("widget_id", 0)
        val manager = AppWidgetManager.getInstance(context)

        val config = AppContainer.widgetConfigRepository.getByWidgetId(widgetId).getOrNull()
            ?: return@withContext Result.success()

        // キャッシュが空のままランダム表示になっている場合は、ここで作り直す
        if (config.getCachedQuestionIdList().isEmpty()) {
            AppContainer.updateWidgetCacheUseCase(widgetId)
        }

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
