package com.example.ailearning.presentation.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ailearning.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 問題が少ない学習セットへ、裏でAI問題を先に生成しておくワーカー。
 *
 * - 学習セット作成直後
 * - アプリ起動時
 * - 定期実行（6時間ごと）
 * - ウィジェットのキャッシュが空のとき
 * - ゲートで未出題の問題が無くなったとき（force=true）
 *
 * に登録され、「未回答」の問題数が閾値未満のセットだけを補充する。
 * 合計件数ではなく未回答数で判定するため、全部答えた後でも補充が止まらない。
 * 生成に失敗した場合は例外を投げて WorkManager にリトライさせる（握りつぶさない）。
 */
class QuestionPrefetchWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val targetSetId = inputData.getString(KEY_LEARNING_SET_ID)
        val force = inputData.getBoolean(KEY_FORCE, false)
        val explicitCount = inputData.getInt(KEY_COUNT, 0)

        val result = runCatching {
            if (explicitCount > 0 && targetSetId != null) {
                // ユーザーが問題数を指定してバックグラウンド生成した場合。
                // 閾値チェックを無視して、指定された問数をちょうど作る。
                generateExactly(targetSetId, explicitCount)
                1
            } else {
                prefetch(targetSetId, force)
            }
        }

        if (result.isFailure) {
            // 一時的なネットワーク・レート制限なら数回までリトライ
            return@withContext if (targetSetId != null && runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
        Result.success()
    }

    /** 指定された問数を生成する（MAX_GENERATE_COUNT ずつ分割）。 */
    private suspend fun generateExactly(targetSetId: String, count: Int) {
        val generate = AppContainer.generateQuestionsUseCase
        var remaining = count
        while (remaining > 0) {
            val chunk = remaining.coerceAtMost(MAX_GENERATE_COUNT)
            val result = generate(targetSetId, chunk)
            if (result.isFailure) throw result.exceptionOrNull() ?: Exception("question generation failed")
            remaining -= chunk
        }
    }

    /** @return 生成を試みたセット数 */
    private suspend fun prefetch(targetSetId: String?, force: Boolean): Int {
        val questionRepository = AppContainer.questionRepository
        val learningSetRepository = AppContainer.learningSetRepository
        val generate = AppContainer.generateQuestionsUseCase
        val prefs = AppContainer.settingsPreferences

        // 補充目標件数（「未回答」の問題数ベース）。
        // 合計件数で判定すると全部答えた後に補充が止まり、既出問題の再出題ばかりになるため。
        val targetCount = prefs.prefetchTargetCount.coerceAtLeast(10)

        val sets = if (targetSetId != null) {
            listOfNotNull(learningSetRepository.getByIdOnce(targetSetId).getOrNull())
        } else {
            learningSetRepository.getAllActive().first()
        }

        var attempted = 0
        val failures = mutableListOf<Exception>()
        for (set in sets) {
            if (attempted >= MAX_GENERATIONS_PER_RUN) break

            val unanswered = questionRepository.countUnansweredByLearningSet(set.id).getOrDefault(0)
            if (unanswered >= targetCount) continue

            val needed = (targetCount - unanswered).coerceAtMost(MAX_GENERATE_COUNT)
            val result = generate(set.id, needed)
            if (result.isSuccess) {
                attempted++
            } else {
                // 失敗を握りつぶさない（握りつぶすと WorkManager が成功扱いして
                // リトライされず、問題が増えないまま気づかれない）
                failures.add(
                    result.exceptionOrNull() as? Exception
                        ?: Exception("question generation failed for ${set.id}")
                )
            }
        }
        if (failures.isNotEmpty()) throw failures.first()
        return attempted
    }

    companion object {
        const val KEY_LEARNING_SET_ID = "learning_set_id"
        const val KEY_FORCE = "force"

        /** ユーザーが指定した生成問数（0 は未指定 = 閾値ベースの補充） */
        const val KEY_COUNT = "count"

        /** 1回の生成で作る最大問数 */
        const val MAX_GENERATE_COUNT = 20

        /** 1回の実行で生成する最大セット数（API 消費の暴走を防ぐ） */
        const val MAX_GENERATIONS_PER_RUN = 10
    }
}

/**
 * 事前生成の WorkManager 登録をまとめたオブジェクト。
 */
object QuestionPrefetchScheduler {

    private const val UNIQUE_PREFIX = "question_prefetch_"
    private const val UNIQUE_ALL = "question_prefetch_all"
    private const val UNIQUE_PERIODIC = "question_prefetch_periodic"

    private fun workManager(context: Context): WorkManager =
        WorkManager.getInstance(context)

    private fun constraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    /** 特定セットの事前生成を依頼する（作成直後・ウィジェット用）。 */
    fun requestPrefetch(context: Context, learningSetId: String, force: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<QuestionPrefetchWorker>()
            .setInputData(
                Data.Builder()
                    .putString(QuestionPrefetchWorker.KEY_LEARNING_SET_ID, learningSetId)
                    .putBoolean(QuestionPrefetchWorker.KEY_FORCE, force)
                    .build()
            )
            .setConstraints(constraints())
            .build()
        workManager(context).enqueueUniqueWork(UNIQUE_PREFIX + learningSetId, ExistingWorkPolicy.KEEP, request)
    }

    /**
     * ユーザーが問題数を指定してバックグラウンド生成を依頼する（問題一覧ページ用）。
     * 既存の事前生成チェーンがあればその後ろに連結するので、
     * observePreparing() が生成中の表示を自動でカバーする。
     */
    fun requestGeneration(context: Context, learningSetId: String, count: Int) {
        val request = OneTimeWorkRequestBuilder<QuestionPrefetchWorker>()
            .setInputData(
                Data.Builder()
                    .putString(QuestionPrefetchWorker.KEY_LEARNING_SET_ID, learningSetId)
                    .putInt(QuestionPrefetchWorker.KEY_COUNT, count.coerceIn(1, 100))
                    .build()
            )
            .setConstraints(constraints())
            .build()
        workManager(context).enqueueUniqueWork(
            UNIQUE_PREFIX + learningSetId,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    /** 全セットの一括チェック（アプリ起動時）。実行中なら再登録しない。 */
    fun requestPrefetchAll(context: Context) {
        val request = OneTimeWorkRequestBuilder<QuestionPrefetchWorker>()
            .setConstraints(constraints())
            .build()
        workManager(context).enqueueUniqueWork(UNIQUE_ALL, ExistingWorkPolicy.KEEP, request)
    }

    /** 定期チェック（2時間ごと）。既存スケジュールは維持。 */
    fun ensurePeriodicTopUp(context: Context) {
        val request = PeriodicWorkRequestBuilder<QuestionPrefetchWorker>(2, TimeUnit.HOURS)
            .setConstraints(constraints())
            .build()
        workManager(context).enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** セットの事前生成が動いているか（実行中/待機中）を Flow で通知する。 */
    fun observePreparing(context: Context, learningSetId: String): kotlinx.coroutines.flow.Flow<Boolean> =
        workManager(context).getWorkInfosForUniqueWorkFlow(UNIQUE_PREFIX + learningSetId)
            .map { infos ->
                infos.any {
                    it.state == androidx.work.WorkInfo.State.ENQUEUED ||
                        it.state == androidx.work.WorkInfo.State.RUNNING
                }
            }

    /** 生成の状態（問題一覧ページ用。失敗も検出できる）。 */
    fun observeGenerationState(context: Context, learningSetId: String): kotlinx.coroutines.flow.Flow<GenerationState> =
        workManager(context).getWorkInfosForUniqueWorkFlow(UNIQUE_PREFIX + learningSetId)
            .map { infos ->
                when {
                    infos.any {
                        it.state == androidx.work.WorkInfo.State.ENQUEUED ||
                            it.state == androidx.work.WorkInfo.State.RUNNING
                    } -> GenerationState.RUNNING
                    infos.any { it.state == androidx.work.WorkInfo.State.FAILED } -> GenerationState.FAILED
                    else -> GenerationState.IDLE
                }
            }
}

/** バックグラウンド生成の状態 */
enum class GenerationState { IDLE, RUNNING, FAILED }
