package com.example.ailearning.domain.usecase

import com.example.ailearning.data.local.prefs.SettingsPreferences
import com.example.ailearning.domain.model.AnswerHistory
import com.example.ailearning.domain.model.Question
import com.example.ailearning.domain.repository.AnswerHistoryRepository
import com.example.ailearning.domain.repository.LearningSetRepository
import com.example.ailearning.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * ゲートで出す問題と、その取得状況。
 *
 * @param question 出題する問題。用意できなければ null（呼び出し側はフェイルオープンする）
 * @param setId 出題元の学習セットID
 * @param poolExhausted 新規に 出せる問題が無く、既出の問題を再利用した場合 true。
 *                      呼び出し側はこれを見て問題の自動補充を依頼できる
 */
data class FocusGateQuestion(
    val question: Question?,
    val setId: String?,
    val poolExhausted: Boolean
)

/**
 * ゲートで出す問題を1問取得する。
 *
 * - 設定で学習セットが指定されていればそこから
 * - 未指定なら問題が存在するセットから自動選択
 * - excludeIds（表示済みID）はDBから多めに引いてから除外し、
 *   同じ問題が連続・再表示されないようにする
 * - どうしても新規が無い（問題数が少ない）場合のみランダムに再利用する
 */
class GetFocusGateQuestionUseCase @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val learningSetRepository: LearningSetRepository,
    private val settingsPreferences: SettingsPreferences
) {
    suspend operator fun invoke(excludeIds: Set<String> = emptySet()): FocusGateQuestion {
        val setId = settingsPreferences.focusLearningSetId.takeIf { it.isNotBlank() }
            ?: findSetIdWithQuestions()
            ?: return FocusGateQuestion(null, null, poolExhausted = false)

        val candidates = questionRepository.getRandomQuestions(setId, FETCH_LIMIT)
            .getOrDefault(emptyList())
        if (candidates.isEmpty()) return FocusGateQuestion(null, setId, poolExhausted = false)

        // 除外条件を段階的に緩める。
        // 1) このゲートで出題済み + 最近30問 を避ける
        // 2) 出題済み + 直近10問 のみ避ける（問題数が少ないセット向け）
        // 3) このゲートで出題済みだけ避ける（同一ゲート内の重複は絶対に防ぐ）
        // 4) それでも無ければランダム（= 問題数が足りず再利用。補充が必要）
        val recent = settingsPreferences.getFocusRecentQuestionIdList()
        val strictExclude = excludeIds + recent.takeLast(RECENT_AVOID_STRICT)
        val relaxedExclude = excludeIds + recent.takeLast(RECENT_AVOID_RELAXED)

        val picked = FocusLockPolicy.pickNext(candidates, strictExclude) { it.id }
            ?: FocusLockPolicy.pickNext(candidates, relaxedExclude) { it.id }
            ?: FocusLockPolicy.pickNext(candidates, excludeIds) { it.id }

        return if (picked != null) {
            FocusGateQuestion(picked, setId, poolExhausted = false)
        } else {
            FocusGateQuestion(candidates.random(), setId, poolExhausted = true)
        }
    }

    private suspend fun findSetIdWithQuestions(): String? {
        val sets = runCatching { learningSetRepository.getAllActive().first() }
            .getOrDefault(emptyList())
        for (set in sets) {
            val count = questionRepository.countByLearningSet(set.id).getOrDefault(0)
            if (count > 0) return set.id
        }
        return null
    }

    companion object {
        /** 1回の取得で引く問題数。多めに引いて除外後の選択肢を確保する */
        private const val FETCH_LIMIT = 30

        /** 直近この件数は必ず避ける */
        private const val RECENT_AVOID_STRICT = 30

        /** 問題数が少ないセットではここまで緩める */
        private const val RECENT_AVOID_RELAXED = 10
    }
}

/**
 * 正解したアプリだけの解除セッションを開始する（v1.6.3 からアプリごと）。
 * 有効期間の設定が「次のロックまで」なら期限なし（ロックで無効化）、
 * 分指定ならその時刻まで有効。他のアプリの解除状態には影響しない。
 */
class GrantFocusUnlockUseCase @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) {
    suspend operator fun invoke(packageName: String) {
        val durationMinutes = settingsPreferences.focusUnlockDurationMinutes
        val until = if (durationMinutes <= 0) 0L else System.currentTimeMillis() + durationMinutes * 60_000L
        settingsPreferences.setPackageUnlocked(packageName, until)
        settingsPreferences.incrementFocusUnlockCount()
    }
}

/**
 * ゲートでの回答を学習履歴に記録する（統計に反映される）。
 */
class RecordFocusAnswerUseCase @Inject constructor(
    private val answerHistoryRepository: AnswerHistoryRepository
) {
    suspend operator fun invoke(question: Question, selected: Int, isCorrect: Boolean, responseTimeMs: Long) {
        runCatching {
            answerHistoryRepository.insert(
                AnswerHistory(
                    questionId = question.id,
                    learningSetId = question.learningSetId,
                    selectedAnswer = selected,
                    isCorrect = isCorrect,
                    responseTimeMs = responseTimeMs,
                    sessionId = "focus_${System.currentTimeMillis() / 60_000}",
                    source = SOURCE_FOCUS_LOCK
                )
            )
        }
    }

    companion object {
        const val SOURCE_FOCUS_LOCK = "focus_lock"
    }
}
