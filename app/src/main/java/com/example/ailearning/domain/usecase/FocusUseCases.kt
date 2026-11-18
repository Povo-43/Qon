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
 * ゲートで出す問題を1問取得する。
 *
 * - 設定で学習セットが指定されていればそこから
 * - 未指定なら問題が存在するセットから自動選択
 * - excludeIds（表示済みID）はDBから多めに引いてから除外し、
 *   同じ問題が連続・再表示されないようにする
 * - どうしても新規が無い（問題数が少ない）場合のみランダムに再利用する
 * - 見つからなければ null（呼び出し側はフェイルオープンする）
 */
class GetFocusGateQuestionUseCase @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val learningSetRepository: LearningSetRepository,
    private val settingsPreferences: SettingsPreferences
) {
    suspend operator fun invoke(excludeIds: Set<String> = emptySet()): Question? {
        val setId = settingsPreferences.focusLearningSetId.takeIf { it.isNotBlank() }
            ?: findSetIdWithQuestions()
            ?: return null

        val candidates = questionRepository.getRandomQuestions(setId, FETCH_LIMIT)
            .getOrDefault(emptyList())
        if (candidates.isEmpty()) return null

        return FocusLockPolicy.pickNext(candidates, excludeIds) { it.id }
            ?: candidates.random()
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
    }
}

/**
 * 正解後、解除セッションを開始する。
 * 有効期間の設定が「次のロックまで」なら期限なし（ロックで無効化）、
 * 分指定ならその時刻まで有効。
 */
class GrantFocusUnlockUseCase @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) {
    suspend operator fun invoke() {
        val durationMinutes = settingsPreferences.focusUnlockDurationMinutes
        val until = if (durationMinutes <= 0) 0L else System.currentTimeMillis() + durationMinutes * 60_000L
        settingsPreferences.setFocusUnlocked(unlocked = true, validUntil = until)
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
