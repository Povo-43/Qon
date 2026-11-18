package com.example.ailearning.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ailearning.data.local.prefs.SettingsPreferences
import com.example.ailearning.di.AppContainer
import com.example.ailearning.domain.model.FocusBlockedApp
import com.example.ailearning.domain.model.LearningSet
import com.example.ailearning.domain.usecase.FocusLockPolicy
import com.example.ailearning.presentation.focus.FocusAccessibilityService
import com.example.ailearning.presentation.focus.FocusDiagnostics
import com.example.ailearning.presentation.focus.FocusGateController
import com.example.ailearning.presentation.focus.FocusServiceState
import com.example.ailearning.presentation.focus.FocusServiceStatus
import com.example.ailearning.presentation.work.QuestionPrefetchScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** アプリ選択ダイアログ用のエントリ */
data class InstalledAppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

class FocusLockViewModel : ViewModel() {

    private val prefs: SettingsPreferences get() = AppContainer.settingsPreferences

    private val _focusEnabled = MutableStateFlow(false)
    val focusEnabled: StateFlow<Boolean> = _focusEnabled.asStateFlow()

    private val _learningSetId = MutableStateFlow("")
    val learningSetId: StateFlow<String> = _learningSetId.asStateFlow()

    private val _learningSets = MutableStateFlow<List<LearningSet>>(emptyList())
    val learningSets: StateFlow<List<LearningSet>> = _learningSets.asStateFlow()

    private val _requiredCorrect = MutableStateFlow(1)
    val requiredCorrect: StateFlow<Int> = _requiredCorrect.asStateFlow()

    private val _unlockDurationMinutes = MutableStateFlow(0)
    val unlockDurationMinutes: StateFlow<Int> = _unlockDurationMinutes.asStateFlow()

    private val _showCorrectAnswer = MutableStateFlow(true)
    val showCorrectAnswer: StateFlow<Boolean> = _showCorrectAnswer.asStateFlow()

    private val _blockedApps = MutableStateFlow<List<FocusBlockedApp>>(emptyList())
    val blockedApps: StateFlow<List<FocusBlockedApp>> = _blockedApps.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppEntry>?>(null)
    val installedApps: StateFlow<List<InstalledAppEntry>?> = _installedApps.asStateFlow()

    private val _serviceState = MutableStateFlow(FocusServiceState.DISABLED)
    val serviceState: StateFlow<FocusServiceState> = _serviceState.asStateFlow()

    /** 診断カードに表示する動作記録 */
    private val _diagnostics = MutableStateFlow("")
    val diagnostics: StateFlow<String> = _diagnostics.asStateFlow()

    /** テスト実行できなかったときなどの一時メッセージ */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private val _unlockCount = MutableStateFlow(0)
    val unlockCount: StateFlow<Int> = _unlockCount.asStateFlow()

    private val _wrongCount = MutableStateFlow(0)
    val wrongCount: StateFlow<Int> = _wrongCount.asStateFlow()

    init {
        refresh()

        AppContainer.focusRepository.getBlockedApps()
            .onEach { _blockedApps.value = it }
            .launchIn(viewModelScope)

        AppContainer.learningSetRepository.getAllActive()
            .onEach { _learningSets.value = it }
            .launchIn(viewModelScope)
    }

    /** 画面復帰時などに呼んで状態を取り直す */
    fun refresh() {
        _focusEnabled.value = prefs.focusEnabled
        _learningSetId.value = prefs.focusLearningSetId
        _requiredCorrect.value = prefs.focusRequiredCorrect
        _unlockDurationMinutes.value = prefs.focusUnlockDurationMinutes
        _showCorrectAnswer.value = prefs.focusShowCorrectAnswer
        _unlockCount.value = prefs.focusUnlockCount
        _wrongCount.value = prefs.focusWrongCount

        val context = runCatching { AppContainer.appContext }.getOrNull()
        if (context != null) {
            _serviceState.value = FocusServiceStatus.state(context)
            _diagnostics.value = FocusDiagnostics.summary(context, _serviceState.value)
        }
    }

    fun consumeNotice() {
        _notice.value = null
    }

    fun setEnabled(enabled: Boolean) {
        prefs.setFocusEnabled(enabled)
        _focusEnabled.value = enabled
        if (!enabled) FocusGateController.dismiss()
    }

    /** 解除に必要な正解数（1〜5） */
    fun setRequiredCorrect(count: Int) {
        prefs.setFocusRequiredCorrect(count)
        _requiredCorrect.value = count
    }

    /** 解除の有効期間（分）。0 = 次のロックまで */
    fun setUnlockDurationMinutes(minutes: Int) {
        prefs.setFocusUnlockDurationMinutes(minutes)
        _unlockDurationMinutes.value = minutes
    }

    /** 誤答時に正解と解説を表示するか */
    fun setShowCorrectAnswer(show: Boolean) {
        prefs.setFocusShowCorrectAnswer(show)
        _showCorrectAnswer.value = show
    }

    /** 対象アプリの有効/無効を切り替える（一時的に exempt する） */
    fun setBlockedAppEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            AppContainer.focusRepository.setBlockedAppEnabled(packageName, enabled)
        }
    }

    /** ゲートの問題源となる学習セット。空文字は「自動」 */
    fun selectLearningSet(setId: String) {
        prefs.setFocusLearningSetId(setId)
        _learningSetId.value = setId
        // ゲートで即座に問題が出せるよう裏で補充
        if (setId.isNotBlank()) {
            AppContainer.appContext.let { QuestionPrefetchScheduler.requestPrefetch(it, setId) }
        }
    }

    fun addBlockedApp(entry: InstalledAppEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            AppContainer.focusRepository.addBlockedApp(
                FocusBlockedApp(
                    packageName = entry.packageName,
                    label = entry.label,
                    enabled = true
                )
            )
        }
    }

    fun removeBlockedApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            AppContainer.focusRepository.removeBlockedApp(packageName)
        }
    }

    /** インストール済みアプリ一覧を遅延ロード（アイコン付き） */
    fun loadInstalledApps(context: Context) {
        if (_installedApps.value != null) return
        viewModelScope.launch {
            _installedApps.value = emptyList()
            _installedApps.value = withContext(Dispatchers.IO) {
                queryLaunchableApps(context.applicationContext)
            }
        }
    }

    /** テスト表示。サービス未接続なら理由を notice に入れる */
    fun showTestGate() {
        val service = FocusAccessibilityService.instance
        if (service == null) {
            _notice.value = "アクセシビリティサービスが接続されていません。端末設定で Qon 集中モードを一度オフ→オンにしてください。"
            return
        }
        val shown = FocusGateController.show(service, testMode = true)
        if (!shown) {
            _notice.value = "ゲートを表示できませんでした。少し待ってからもう一度お試しください。"
        }
    }

    private fun queryLaunchableApps(context: Context): List<InstalledAppEntry> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val ownPackage = context.packageName

        return runCatching {
            pm.queryIntentActivities(launcherIntent, 0)
        }.getOrDefault(emptyList())
            .asSequence()
            .mapNotNull { it.activityInfo }
            .filter { it.packageName != ownPackage }
            .distinctBy { it.packageName }
            .filter { !FocusLockPolicy.isProtectedPackage(it.packageName, ownPackage) }
            .map { info ->
                val icon = runCatching { pm.getApplicationIcon(info.packageName) }.getOrNull()
                InstalledAppEntry(
                    packageName = info.packageName,
                    label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(info.packageName),
                    icon = loadIconSafely(icon)
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun loadIconSafely(drawable: Drawable?): ImageBitmap? {
        if (drawable == null) return null
        return runCatching {
            val bitmap = drawable.toBitmap(96, 96, Bitmap.Config.ARGB_8888)
            bitmap.asImageBitmap()
        }.getOrNull()
    }
}
