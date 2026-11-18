package com.example.ailearning.presentation.focus

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.example.ailearning.data.local.prefs.SettingsPreferences
import com.example.ailearning.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 集中モードの本体（v1.6.0）。
 *
 * - 前面アプリのパッケージ名のみを監視（canRetrieveWindowContent=false のため
 *   画面の内容は一切読み取らない）
 * - 画面ロック解除のたびに解除セッションをリセットする
 * - ロック対象アプリが前面に来たら全画面ゲートを表示する
 */
class FocusAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var prefs: SettingsPreferences

    private var blockedPackages: Set<String> = emptySet()
    private var focusEnabledCached = false

    /** 現在のキーボードのパッケージ（ゲートの閉じ判定から除外する） */
    private var defaultImePackage: String? = null

    /** 設定画面のテスト表示などに使うインスタンス */
    companion object {
        @Volatile
        var instance: FocusAccessibilityService? = null
            private set

        /** このサービスが有効化されているか */
        fun isRunning(): Boolean = instance != null
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
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
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        prefs = SettingsPreferences(applicationContext)
        focusEnabledCached = prefs.focusEnabled
        defaultImePackage = runCatching {
            android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.DEFAULT_INPUT_METHOD
            )?.substringBefore('/')
        }.getOrNull()

        // ロック対象アプリの変更を常時反映
        serviceScope.launch {
            runCatching {
                AppContainer.focusRepository.getBlockedApps().collect { apps ->
                    blockedPackages = apps.filter { it.enabled }.map { it.packageName }.toSet()
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val foregroundPackage = event.packageName?.toString() ?: return
        // 自アプリ（ゲート自身のウィンドウイベント）は無視してループを防ぐ
        if (foregroundPackage == packageName) return

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
            FocusGateController.show(this)
        }
    }

    override fun onInterrupt() {
        FocusGateController.dismiss()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        runCatching { unregisterReceiver(screenStateReceiver) }
        FocusGateController.dismiss()
        serviceScope.cancel()
        if (instance === this) instance = null
    }
}
