package com.example.ailearning.presentation.focus

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.example.ailearning.data.local.prefs.SettingsPreferences
import com.example.ailearning.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 集中モードの本体。
 *
 * - 前面アプリのパッケージ名のみを監視（canRetrieveWindowContent=false のため
 *   画面の内容は一切読み取らない）
 * - 画面ロック解除のたびに解除セッションをリセットする
 * - ロック対象アプリが前面に来たら全画面ゲートを表示する
 *
 * 重要:
 * このサービスのコールバックから例外を投げると、Android はサービスを
 * 「クラッシュしたサービス」として扱い、設定をオフ→オンし直すまで復帰しなくなる。
 * （Xiaomi の設定画面では「このサービスは機能していません」と表示される）
 * そのため、すべてのコールバックを try/catch で保護し、
 * 例外は端末内の診断ログにだけ残す。
 */
class FocusAccessibilityService : AccessibilityService() {

    /**
     * 再バインド時に作り直す。onUnbind / onDestroy で cancel するため、
     * 使い回すと再バインド後にロック対象アプリの監視が止まる。
     */
    private var serviceScope: CoroutineScope? = null

    private val prefs: SettingsPreferences by lazy { SettingsPreferences(applicationContext) }

    private var blockedPackages: Set<String> = emptySet()
    private var focusEnabledCached = false
    private var receiverRegistered = false

    /** 現在のキーボードのパッケージ（ゲートの閉じ判定から除外する） */
    private var defaultImePackage: String? = null

    /** 診断ログの書き込みを減らすため、最後に記録したパッケージを覚えておく */
    private var lastLoggedPackage: String? = null

    companion object {
        /** 設定画面のテスト表示などに使うインスタンス */
        @Volatile
        var instance: FocusAccessibilityService? = null
            private set

        /** このサービスが接続され動作しているか */
        fun isRunning(): Boolean = instance != null
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                when (intent?.action) {
                    // 画面が消えた = ロックへ移行。
                    // 「次のロックまで」モードでは解除セッションを無効化、
                    // 時間指定モードではセッションは残り、有効期限のみで判断する
                    Intent.ACTION_SCREEN_OFF -> {
                        if (prefs.focusUnlockDurationMinutes <= 0) {
                            prefs.setFocusUnlocked(false)
                        }
                        FocusGateController.dismiss()
                    }
                    // ロック解除 = 「次のロックまで」モードでは毎回ここから「未解除」に戻る
                    Intent.ACTION_USER_PRESENT -> {
                        if (prefs.focusUnlockDurationMinutes <= 0) {
                            prefs.setFocusUnlocked(false)
                        }
                        FocusGateController.dismiss()
                    }
                }
            } catch (t: Throwable) {
                FocusDiagnostics.onError(this@FocusAccessibilityService, "screenStateReceiver", t)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            instance = this

            // XML の設定を念のため実行時にも反映する（端末によって XML が
            // 正しく読まれないことがあるため）。失敗しても続行する。
            runCatching {
                serviceInfo?.let { info ->
                    info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                    info.notificationTimeout = 100
                    serviceInfo = info
                }
            }

            defaultImePackage = readDefaultImePackage()
            FocusDiagnostics.onConnected(this)

            registerScreenStateReceiver()
            startObservingBlockedApps()
        } catch (t: Throwable) {
            // ここで throw するとサービスがクラッシュ扱いになり復帰できなくなる
            FocusDiagnostics.onError(this, "onServiceConnected", t)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            handleAccessibilityEvent(event)
        } catch (t: Throwable) {
            FocusDiagnostics.onError(this, "onAccessibilityEvent", t)
        }
    }

    private fun handleAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val foregroundPackage = event.packageName?.toString() ?: return
        // 自アプリ（ゲート自身のウィンドウイベント）は無視してループを防ぐ
        if (foregroundPackage == packageName) return

        if (lastLoggedPackage != foregroundPackage) {
            lastLoggedPackage = foregroundPackage
            FocusDiagnostics.onEvent(this, foregroundPackage)
        }

        // ロック画面が表示中 → ゲートは出さない
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        if (keyguard?.isKeyguardLocked == true) {
            if (FocusGateController.isShowing) FocusGateController.dismiss()
            return
        }

        // 設定は都度読む（SharedPreferences はメモリ読みなので軽量）
        focusEnabledCached = prefs.focusEnabled
        if (!focusEnabledCached) {
            if (FocusGateController.isShowing) FocusGateController.dismiss()
            return
        }

        // 期限切れの解除セッションが残っていれば掃除する
        if (prefs.focusUnlocked && !prefs.isFocusUnlockSessionActive()) {
            prefs.setFocusUnlocked(false)
        }

        // ゲート表示中に別アプリ（着信・ランチャー・通知など）が前面に出たら閉じる。
        // ゲートが画面を塞ぎ続けて電話に出られない、といった事故を防ぐため。
        // IME（キーボード）は無視する。
        if (FocusGateController.isShowing) {
            val isIme = foregroundPackage == defaultImePackage
            if (!isIme && foregroundPackage !in blockedPackages) {
                if (FocusGateController.canDismissByForeground()) {
                    FocusGateController.dismiss()
                    return
                }
            }
        }

        // 解除セッション中（前回正解してから有効期限内）は何もしない
        if (prefs.isFocusUnlockSessionActive()) return

        if (foregroundPackage in blockedPackages) {
            runCatching { FocusGateController.show(this) }
                .onFailure { FocusDiagnostics.onError(this, "showGate", it) }
        }
    }

    override fun onInterrupt() {
        runCatching { FocusGateController.dismiss() }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown("onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown("onDestroy")
        super.onDestroy()
    }

    // ------------------------------------------------------------ setup

    private fun registerScreenStateReceiver() {
        if (receiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            // Android 14+ ではフラグ未指定の registerReceiver が
            // SecurityException になる場合があるため明示する
            ContextCompat.registerReceiver(
                this,
                screenStateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        } catch (t: Throwable) {
            FocusDiagnostics.onError(this, "registerReceiver", t)
        }
    }

    private fun startObservingBlockedApps() {
        serviceScope?.cancel()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        serviceScope = scope

        // 起動直後のイベントを取りこぼさないよう、まず1回読み込む
        scope.launch {
            runCatching {
                blockedPackages = AppContainer.focusRepository.getEnabledBlockedAppsOnce()
                    .map { it.packageName }
                    .toSet()
            }.onFailure { FocusDiagnostics.onError(this@FocusAccessibilityService, "loadBlockedApps", it) }

            runCatching {
                AppContainer.focusRepository.getBlockedApps().collect { apps ->
                    blockedPackages = apps.filter { it.enabled }.map { it.packageName }.toSet()
                }
            }.onFailure { FocusDiagnostics.onError(this@FocusAccessibilityService, "observeBlockedApps", it) }
        }
    }

    private fun readDefaultImePackage(): String? = runCatching {
        android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.DEFAULT_INPUT_METHOD
        )?.substringBefore('/')
    }.getOrNull()

    private fun teardown(reason: String) {
        try {
            FocusDiagnostics.onUnbound(this, reason)
            if (receiverRegistered) {
                runCatching { unregisterReceiver(screenStateReceiver) }
                receiverRegistered = false
            }
            runCatching { FocusGateController.dismiss() }
            serviceScope?.cancel()
            serviceScope = null
            blockedPackages = emptySet()
            lastLoggedPackage = null
            if (instance === this) instance = null
        } catch (t: Throwable) {
            FocusDiagnostics.onError(this, "teardown", t)
        }
    }
}
