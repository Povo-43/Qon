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
 *
 * に登録され、閾値未満のセットだけを補充する。
 */
class QuestionPrefetchWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val targetSetId = inputData.getString(KEY_LEARNING_SET_ID)

        val result = runCatching { prefetch(targetSetId) }

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

    /** @return 生成を試みたセット数 */
    private suspend fun prefetch(targetSetId: String?): Int {
        val questionRepository = AppContainer.questionRepository
        val learningSetRepository = AppContainer.learningSetRepository
        val generate = AppContainer.generateQuestionsUseCase

        val sets = if (targetSetId != null) {
            listOfNotNull(learningSetRepository.getByIdOnce(targetSetId).getOrNull())
        } else {
            learningSetRepository.getAllActive().first()
        }

        var attempted = 0
        for (set in sets) {
            if (attempted >= MAX_GENERATIONS_PER_RUN) break

            val existing = questionRepository.countByLearningSet(set.id).getOrDefault(0)
            if (existing >= PREFILL_THRESHOLD) continue

            val needed = (PREFILL_THRESHOLD - existing).coerceAtMost(MAX_GENERATE_COUNT)
            val result = generate(set.id, needed)
            if (result.isSuccess) attempted++
        }
        return attempted
    }

    companion object {
        const val KEY_LEARNING_SET_ID = "learning_set_id"

        /** この件数を下回るセットだけを補充する */
        const val PREFILL_THRESHOLD = 10

        /** 1回の生成で作る最大問数 */
        const val MAX_GENERATE_COUNT = 10

        /** 1回の実行で生成する最大セット数（API 消費の暴走を防ぐ） */
        const val MAX_GENERATIONS_PER_RUN = 5
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
    fun requestPrefetch(context: Context, learningSetId: String) {
        val request = OneTimeWorkRequestBuilder<QuestionPrefetchWorker>()
            .setInputData(Data.Builder().putString(QuestionPrefetchWorker.KEY_LEARNING_SET_ID, learningSetId).build())
            .setConstraints(constraints())
            .build()
        workManager(context).enqueueUniqueWork(UNIQUE_PREFIX + learningSetId, ExistingWorkPolicy.KEEP, request)
    }

    /** 全セットの一括チェック（アプリ起動時）。実行中なら再登録しない。 */
    fun requestPrefetchAll(context: Context) {
        val request = OneTimeWorkRequestBuilder<QuestionPrefetchWorker>()
            .setConstraints(constraints())
            .build()
        workManager(context).enqueueUniqueWork(UNIQUE_ALL, ExistingWorkPolicy.KEEP, request)
    }

    /** 定期チェック（6時間ごと）。既存スケジュールは維持。 */
    fun ensurePeriodicTopUp(context: Context) {
        val request = PeriodicWorkRequestBuilder<QuestionPrefetchWorker>(6, TimeUnit.HOURS)
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
}
