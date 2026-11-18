package com.example.ailearning.presentation.focus

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/**
 * アクセシビリティサービスの状態。
 *
 * 「設定では有効なのに接続されていない」という状態を区別できるようにするのが要点。
 * この状態は MIUI / HyperOS で特によく起こり、設定をオフ→オンし直すまで復帰しない。
 */
enum class FocusServiceState(val label: String) {
    /** 端末設定で許可されていない */
    DISABLED("無効"),

    /** 端末設定では許可されているが、サービスが接続されていない */
    ENABLED_NOT_CONNECTED("有効（未接続）"),

    /** 許可されていて、実際に動作している */
    CONNECTED("有効（動作中）")
}

object FocusServiceStatus {

    fun state(context: Context): FocusServiceState {
        if (FocusAccessibilityService.isRunning()) return FocusServiceState.CONNECTED
        return if (isEnabled(context)) {
            FocusServiceState.ENABLED_NOT_CONNECTED
        } else {
            FocusServiceState.DISABLED
        }
    }

    /**
     * 端末設定でこのサービスが有効になっているか。
     * 自プロセスのインスタンス有無ではなく、システム側の有効一覧で判定する。
     */
    fun isEnabled(context: Context): Boolean = runCatching {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val target = ComponentName(context.packageName, FocusAccessibilityService::class.java.name)
        manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                val serviceInfo = info.resolveInfo?.serviceInfo ?: return@any false
                serviceInfo.packageName == target.packageName && serviceInfo.name == target.className
            }
    }.getOrDefault(false)
}

/**
 * 集中モードが止められやすい端末（MIUI / HyperOS など）の設定画面を開く。
 * 開けなかった場合はアプリ情報画面にフォールバックする。
 */
object FocusOemSettings {

    private val AUTOSTART_COMPONENTS = listOf(
        // Xiaomi (MIUI / HyperOS)
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartActivity"),
        // 参考: 他の中国系 OEM
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
    )

    fun openAccessibilitySettings(context: Context): Boolean =
        start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun openAppDetails(context: Context): Boolean = start(
        context,
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
    )

    fun openBatterySettings(context: Context): Boolean =
        start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            || start(context, Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))

    /** 自動起動の許可画面。見つからなければアプリ情報を開く。 */
    fun openAutostart(context: Context): Boolean {
        for (component in AUTOSTART_COMPONENTS) {
            val intent = Intent().setComponent(component)
            val resolvable = runCatching {
                context.packageManager.resolveActivity(intent, 0) != null
            }.getOrDefault(false)
            if (resolvable && start(context, intent)) return true
        }
        return openAppDetails(context)
    }

    private fun start(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}
