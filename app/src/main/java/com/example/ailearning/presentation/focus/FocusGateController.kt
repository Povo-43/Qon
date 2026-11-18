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
import kotlinx.coroutines.flow.asStateFlow
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
 * - 問題はローカルDBから即座に取得（ネットワーク待ちは発生しない）。
 *   用意できない場合はフェイルオープン（自動で閉じる）
 * - 出題済みの問題IDを保持し、同じ問題の再表示を防ぐ
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

    val isShowing: Boolean get() = hostView != null

    /**
     * ゲートを表示する。すでに表示中なら何もしない。
     *
     * @param serviceContext アクセシビリティサービス自身（TYPE_ACCESSIBILITY_OVERLAY 用）
     */
    fun show(serviceContext: Context, testMode: Boolean = false): Boolean {
        if (hostView != null) return true
        if (System.currentTimeMillis() - lastDismissAt < RE_SHOW_COOLDOWN_MS && !testMode) return false

        this.testMode = testMode
        recordResults = !testMode

        // 設定を反映（必要正解数・誤答時の正解表示）
        val prefs = AppContainer.settingsPreferences
        session.reset(
            requiredCorrect = prefs.focusRequiredCorrect,
            showCorrectAnswer = prefs.focusShowCorrectAnswer
        )

        val host = FocusGateHostView(serviceContext)
        host.setGateContent { AiLearningTheme(darkTheme = true) { FocusGateScreen(this@FocusGateController, testMode) } }

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
        mainHandler.post {
            val host = hostView ?: return@post
            hostView = null
            windowManager?.let { wm ->
                runCatching { wm.removeView(host) }
            }
            windowManager = null
            runCatching { host.destroyHost() }
            lastDismissAt = System.currentTimeMillis()
        }
    }

    // ------------------------------------------------------------ from UI

    /** 選択肢がタップされた。 */
    fun selectAnswer(index: Int) {
        val correct = session.onAnswer(index)
        if (!recordResults) return
        val question = session.currentQuestion() ?: return
        val phase = session.state.value.phase
        val elapsed = (System.currentTimeMillis() - questionShownAtMs).coerceAtLeast(0)
        scope.launch(Dispatchers.IO) {
            runCatching {
                AppContainer.recordFocusAnswerUseCase(question, index, correct, elapsed)
                when (phase) {
                    FocusGateUiState.Phase.UNLOCKED -> AppContainer.grantFocusUnlockUseCase()
                    FocusGateUiState.Phase.RESULT_WRONG ->
                        AppContainer.settingsPreferences.incrementFocusWrongCount()
                    else -> Unit
                }
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
        // 表示済み（このゲート + 過去30問分）を除外して取得
        val exclude = buildSet {
            addAll(session.excludeIds())
            addAll(runCatching { AppContainer.settingsPreferences.getFocusRecentQuestionIds() }.getOrDefault(emptySet()))
        }
        val question = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                runCatching { AppContainer.getFocusGateQuestionUseCase(exclude) }
                    .getOrElse { null }
            }
        }
        if (hostView == null) return // 表示中に閉じられた
        session.onQuestionLoaded(question)
        question?.let {
            runCatching { AppContainer.settingsPreferences.appendFocusRecentQuestionId(it.id) }
        }
    }

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
}
