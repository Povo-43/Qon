package com.example.ailearning.presentation.focus

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.example.ailearning.di.AppContainer
import com.example.ailearning.presentation.ui.theme.AiLearningTheme

/**
 * ゲート（全画面オーバーレイ）の表示・破棄と問題のやり取りを管理する。
 *
 * - TYPE_ACCESSIBILITY_OVERLAY はアクセシビリティサービスのコンテキストからのみ
 *   追加できるため、必ず FocusAccessibilityService を渡すこと
 * - ゲートは「どのアプリのロックか」を持つ（targetPackage）。
 *   正解してもそのアプリだけが解除され、他のアプリはそれぞれ正解が必要（v1.6.3）
 * - 問題はローカルDBから即座に取得（ネットワーク待ちは発生しない）。
 *   用意できない場合はフェイルオープン（自動で閉じる）
 * - 出題済みの問題IDを保持し、同じ問題の再表示を防ぐ。
 *   未出題の問題が無くなったら問題の自動補充を依頼する
 */
object FocusGateController {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val session = FocusGateSession()
    val state: StateFlow<FocusGateUiState> get() = session.state

    private var hostView: FocusGateHostView? = null
    private var windowManager: WindowManager? = null
    private var questionShownAtMs = 0L
    private var recordResults = true
    private var testMode = false
    private var lastDismissAt = 0L
    private var shownAtMs = 0L

    /** このゲートがロックしているアプリ。テスト表示時は null */
    private var gateTargetPackage: String? = null

    /** ゲートに表示するアプリ名（解決できなければ null） */
    private var gateTargetLabel: String? = null

    /** フェイルオープンして通したアプリと時刻。短時間の再表示ループを防ぐ */
    private var lastFailOpenPackage: String? = null
    private var lastFailOpenAt = 0L

    val isShowing: Boolean get() = hostView != null

    /** 現在ゲートが出ている対象アプリ（null はテスト表示） */
    val targetPackage: String? get() = if (isShowing) gateTargetPackage else null

    /**
     * 別アプリが前面に出たことでゲートを閉じてよいか。
     * 表示直後はランチャーの pause イベントなどが遅れて届くことがあるため、
     * 一定時間は誤判定で閉じないようにする（着信時は少し遅れてでも閉じる）。
     */
    fun canDismissByForeground(): Boolean =
        hostView != null && System.currentTimeMillis() - shownAtMs > FOREGROUND_DISMISS_GRACE_MS

    /**
     * ゲートを表示する。すでに同じ対象で表示中なら何もしない。
     *
     * @param serviceContext アクセシビリティサービス自身（TYPE_ACCESSIBILITY_OVERLAY 用）
     * @param targetPackage ロックするアプリ。テスト表示時は null
     * @param ignoreCooldown アプリ切り替え時の付け替えなど、直前の dismiss 直後でも即表示する
     */
    fun show(
        serviceContext: Context,
        targetPackage: String? = null,
        testMode: Boolean = false,
        ignoreCooldown: Boolean = false
    ): Boolean {
        if (hostView != null) return true
        if (!testMode && !ignoreCooldown &&
            System.currentTimeMillis() - lastDismissAt < RE_SHOW_COOLDOWN_MS
        ) return false

        // フェイルオープンで通した直後のアプリには、しばらくゲートを出さない
        // （問題を用意できない状態で表示と解除を繰り返すのを防ぐ）
        if (!testMode && targetPackage != null &&
            targetPackage == lastFailOpenPackage &&
            System.currentTimeMillis() - lastFailOpenAt < FAIL_OPEN_RETRY_MS
        ) return false

        this.testMode = testMode
        recordResults = !testMode
        gateTargetPackage = targetPackage
        gateTargetLabel = targetPackage?.let { resolveAppLabel(serviceContext, it) }

        // 設定を反映（必要正解数・誤答時の正解表示）
        val prefs = AppContainer.settingsPreferences
        session.reset(
            requiredCorrect = prefs.focusRequiredCorrect,
            showCorrectAnswer = prefs.focusShowCorrectAnswer
        )

        val host = FocusGateHostView(serviceContext)
        val label = gateTargetLabel
        host.setGateContent {
            AiLearningTheme(darkTheme = true) {
                FocusGateScreen(this@FocusGateController, testMode, label)
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN

        return try {
            val wm = serviceContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.addView(host, params)
            windowManager = wm
            hostView = host
            shownAtMs = System.currentTimeMillis()

            scope.launch { loadQuestion() }
            observeSession()
            true
        } catch (e: Exception) {
            // オーバーレイを載せられない環境では、以降は静かに諦める（フェイルオープン扱い）
            runCatching { host.destroyHost() }
            hostView = null
            windowManager = null
            false
        }
    }

    fun dismiss() {
        // メインスレッドからは即時に外す。アクセシビリティイベントの直後に
        // 別アプリ用のゲートを付け替える際、post だと間に合わないため。
        if (Looper.myLooper() == Looper.getMainLooper()) {
            dismissInternal()
        } else {
            mainHandler.post { dismissInternal() }
        }
    }

    private fun dismissInternal() {
        val host = hostView ?: return
        hostView = null
        windowManager?.let { wm ->
            runCatching { wm.removeView(host) }
        }
        windowManager = null
        runCatching { host.destroyHost() }
        lastDismissAt = System.currentTimeMillis()
    }

    // ------------------------------------------------------------ from UI

    /** 選択肢がタップされた。 */
    fun selectAnswer(index: Int) {
        val correct = session.onAnswer(index)
        if (!recordResults) return
        val question = session.currentQuestion() ?: return
        val phase = session.state.value.phase
        val target = gateTargetPackage
        val elapsed = (System.currentTimeMillis() - questionShownAtMs).coerceAtLeast(0)
        scope.launch(Dispatchers.IO) {
            runCatching {
                AppContainer.recordFocusAnswerUseCase(question, index, correct, elapsed)
                when (phase) {
                    FocusGateUiState.Phase.UNLOCKED -> target?.let { AppContainer.grantFocusUnlockUseCase(it) }
                    FocusGateUiState.Phase.RESULT_WRONG ->
                        AppContainer.settingsPreferences.incrementFocusWrongCount()
                    else -> Unit
                }
                // 解く量に合わせてストックを維持：回答後、その学習セットの残り問題数をチェックし
                // 目標の半分を下回っていたら裏でAI問題を生成して補充する（時間ベースではなく消費ベース）
                topUpIfLowStock(question.learningSetId)
            }
        }
    }

    /** 結果表示後「次の問題へ」。 */
    fun requestNextQuestion() {
        session.onNextQuestion()
        scope.launch { loadQuestion() }
    }

    /** テストモードの閉じるボタン。 */
    fun dismissTestGate() {
        if (testMode) dismiss()
    }

    // ------------------------------------------------------------ internals

    private suspend fun loadQuestion() {
        questionShownAtMs = System.currentTimeMillis()
        // このゲートで表示済みの問題を必ず除外（過去分の回避はユースケース側で段階的に処理）
        val exclude = session.excludeIds()
        val result = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                runCatching { AppContainer.getFocusGateQuestionUseCase(exclude) }.getOrElse { null }
            }
        }
        if (hostView == null) return // 表示中に閉じられた

        session.onQuestionLoaded(result?.question)
        val setId = result?.setId
        if (result?.poolExhausted == true && setId != null) {
            // 未出題の問題が無くなった → 緊急補充
            requestPrefetch(setId)
        } else if (setId != null) {
            // 「未回答」の残りが目標の半分を下回ったら、裏で補充を開始する
            // （合計件数で判定すると全部答えた後に補充が止まるため）
            val unanswered = AppContainer.questionRepository.countUnansweredByLearningSet(setId).getOrDefault(0)
            val target = AppContainer.settingsPreferences.prefetchTargetCount
            if (unanswered < target / 2) {
                requestPrefetch(setId)
            }
        }
        result?.question?.let {
            runCatching { AppContainer.settingsPreferences.appendFocusRecentQuestionId(it.id) }
        }
    }

    private fun requestPrefetch(setId: String) {
        runCatching {
            com.example.ailearning.presentation.work.QuestionPrefetchScheduler
                .requestPrefetch(AppContainer.appContext, setId, force = true)
        }
    }

    /** 回答後のストック維持。「未回答」の残りが目標の半分を下回ったら裏で補充する。 */
    private suspend fun topUpIfLowStock(setId: String) {
        val unanswered = AppContainer.questionRepository.countUnansweredByLearningSet(setId).getOrDefault(0)
        val target = AppContainer.settingsPreferences.prefetchTargetCount
        if (unanswered < target / 2) {
            requestPrefetch(setId)
        }
    }

    private fun resolveAppLabel(context: Context, packageName: String): String? = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()

    /** フェーズに応じた自動遷移（正解の次問題送り・解除・フェイルオープン）。 */
    private fun observeSession() {
        scope.launch {
            while (isActive && hostView != null) {
                when (state.value.phase) {
                    FocusGateUiState.Phase.RESULT_CORRECT -> {
                        delay(CORRECT_ADVANCE_DELAY_MS)
                        if (hostView != null && state.value.phase == FocusGateUiState.Phase.RESULT_CORRECT) {
                            session.onNextQuestion()
                            loadQuestion()
                        }
                    }
                    FocusGateUiState.Phase.UNLOCKED -> {
                        delay(UNLOCK_DISMISS_DELAY_MS)
                        dismiss()
                        return@launch
                    }
                    FocusGateUiState.Phase.FAIL_OPEN -> {
                        if (!testMode && gateTargetPackage != null) {
                            lastFailOpenPackage = gateTargetPackage
                            lastFailOpenAt = System.currentTimeMillis()
                        }
                        delay(FAIL_OPEN_DISMISS_DELAY_MS)
                        dismiss()
                        return@launch
                    }
                    else -> Unit
                }
                delay(80)
            }
        }
    }

    private const val LOAD_TIMEOUT_MS = 5_000L
    private const val UNLOCK_DISMISS_DELAY_MS = 900L
    private const val FAIL_OPEN_DISMISS_DELAY_MS = 1_800L
    private const val CORRECT_ADVANCE_DELAY_MS = 750L
    private const val RE_SHOW_COOLDOWN_MS = 500L
    /** ゲート表示直後の誤判定防止（ランチャー等の遅延イベント対策） */
    private const val FOREGROUND_DISMISS_GRACE_MS = 800L
    /** フェイルオープンで通した後、同じアプリにゲートを出さない期間 */
    private const val FAIL_OPEN_RETRY_MS = 30_000L
}
