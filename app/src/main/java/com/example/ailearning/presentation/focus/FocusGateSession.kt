package com.example.ailearning.presentation.focus

import com.example.ailearning.domain.model.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ゲート（問題に正解して解除）の状態遷移。UI に依存しないステートマシン。
 *
 * フロー:
 *   LOADING → ANSWERING → (正解, まだ規定回数に満たない) RESULT_CORRECT → LOADING
 *                       → (正解, 規定回数到達)            UNLOCKED
 *                       → (誤答)                          RESULT_WRONG → LOADING
 *   問題を用意できない → FAIL_OPEN（通す）
 *
 * 補足:
 * - onAnswer は ANSWERING 中のみ受け付ける（連打による二重遷移を防ぐ）
 * - 表示済みの問題IDは excludeIds() で取得でき、次の取得時に除外される
 *   （= 同じゲート表示中に同じ問題は二度と出ない）
 */
data class FocusGateUiState(
    val phase: Phase = Phase.LOADING,
    val question: Question? = null,
    val selectedAnswer: Int = -1,
    val isCorrect: Boolean = false,
    /** 表示した問題数（誤答・正答を含む） */
    val attempts: Int = 0,
    /** 連続誤答回数（このゲート表示中に答えた問題のうち正解しなかった数） */
    val wrongCount: Int = 0,
    /** 正解した問題数 */
    val correctCount: Int = 0,
    /** 解除に必要な正解数 */
    val requiredCorrect: Int = 1,
    /** 誤答時に正解と解説を見せるか */
    val showCorrectAnswer: Boolean = true,
    /** フェイルオープン時に出す案内文 */
    val failOpenMessage: String? = null
) {
    enum class Phase {
        LOADING,
        ANSWERING,
        /** 正解（規定回数に未達）。次の問題へ自動遷移する */
        RESULT_CORRECT,
        /** 誤答表示（設定により正解・解説を表示 → 次の問題へ） */
        RESULT_WRONG,
        /** 規定回数の正解に到達 → 解除 */
        UNLOCKED,
        /** 問題が用意できないため通す */
        FAIL_OPEN
    }

    val isBusy: Boolean get() = phase == Phase.LOADING
}

class FocusGateSession {

    private val _state = MutableStateFlow(FocusGateUiState())
    val state: StateFlow<FocusGateUiState> = _state.asStateFlow()

    /** このゲート表示中に出題済みの問題ID（重複出題の完全防止用） */
    private val shownQuestionIds = LinkedHashSet<String>()

    /** ゲートを開くたびに設定を反映してリセットする */
    fun reset(requiredCorrect: Int = 1, showCorrectAnswer: Boolean = true) {
        shownQuestionIds.clear()
        _state.value = FocusGateUiState(
            requiredCorrect = requiredCorrect.coerceIn(1, 10),
            showCorrectAnswer = showCorrectAnswer
        )
    }

    /** 問題が読み込まれた。null は「用意できなかった」（フェイルオープン）。 */
    fun onQuestionLoaded(question: Question?) {
        if (question == null) {
            _state.value = _state.value.copy(
                phase = FocusGateUiState.Phase.FAIL_OPEN,
                failOpenMessage = FAIL_OPEN_MESSAGE
            )
            return
        }
        // 前の問題の選択状態を引きずらないよう、回答関連フィールドは必ず初期化
        shownQuestionIds += question.id
        _state.value = _state.value.copy(
            phase = FocusGateUiState.Phase.ANSWERING,
            question = question,
            selectedAnswer = -1,
            isCorrect = false
        )
    }

    /**
     * 回答を選択。正解なら true。
     * ANSWERING フェーズ以外（連打・結果表示中）では何もせず false を返す。
     */
    fun onAnswer(selected: Int): Boolean {
        val current = _state.value
        if (current.phase != FocusGateUiState.Phase.ANSWERING) return false
        val question = current.question ?: return false

        val isCorrect = question.isCorrectAnswer(selected)
        val correctCount = if (isCorrect) current.correctCount + 1 else current.correctCount
        val wrongCount = if (isCorrect) current.wrongCount else current.wrongCount + 1

        _state.value = when {
            // 規定の正解数に到達 → 解除
            isCorrect && correctCount >= current.requiredCorrect -> current.copy(
                phase = FocusGateUiState.Phase.UNLOCKED,
                selectedAnswer = selected,
                isCorrect = true,
                correctCount = correctCount,
                wrongCount = wrongCount,
                attempts = current.attempts + 1
            )
            // 正解だが続行（複数問モード）
            isCorrect -> current.copy(
                phase = FocusGateUiState.Phase.RESULT_CORRECT,
                selectedAnswer = selected,
                isCorrect = true,
                correctCount = correctCount,
                wrongCount = wrongCount,
                attempts = current.attempts + 1
            )
            // 誤答
            else -> current.copy(
                phase = FocusGateUiState.Phase.RESULT_WRONG,
                selectedAnswer = selected,
                isCorrect = false,
                correctCount = correctCount,
                wrongCount = wrongCount,
                attempts = current.attempts + 1
            )
        }
        return isCorrect
    }

    /** 結果表示後「次の問題へ」。LOADING に戻り、回答状態を完全にクリアする。 */
    fun onNextQuestion() {
        _state.value = _state.value.copy(
            phase = FocusGateUiState.Phase.LOADING,
            selectedAnswer = -1,
            isCorrect = false
        )
    }

    /** このゲート表示中に表示した問題ID（次の取得で除外する） */
    fun excludeIds(): Set<String> = shownQuestionIds.toSet()

    /** 直前に表示した問題（履歴記録用） */
    fun currentQuestion(): Question? = _state.value.question

    companion object {
        const val FAIL_OPEN_MESSAGE = "問題を準備できませんでした\n今回は解除します（通信環境を確認してください）"
    }
}
