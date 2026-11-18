package com.example.ailearning.presentation.focus

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 集中モードの動作記録（端末内のみ）。
 *
 * アクセシビリティサービスが「設定では有効なのに動かない」ときに、
 * 原因をユーザー自身で確認できるようにするための最小限の診断情報。
 * 画面の内容や入力、個人情報は一切記録しない。
 */
object FocusDiagnostics {

    private const val PREFS_NAME = "focus_diagnostics"

    private const val KEY_CONNECTED_AT = "connected_at"
    private const val KEY_CONNECT_COUNT = "connect_count"
    private const val KEY_UNBOUND_AT = "unbound_at"
    private const val KEY_UNBOUND_REASON = "unbound_reason"
    private const val KEY_LAST_EVENT_PACKAGE = "last_event_package"
    private const val KEY_LAST_EVENT_AT = "last_event_at"
    private const val KEY_LAST_ERROR = "last_error"
    private const val KEY_LAST_ERROR_AT = "last_error_at"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** サービスが接続された */
    fun onConnected(context: Context) {
        runCatching {
            val p = prefs(context)
            p.edit()
                .putLong(KEY_CONNECTED_AT, System.currentTimeMillis())
                .putInt(KEY_CONNECT_COUNT, p.getInt(KEY_CONNECT_COUNT, 0) + 1)
                .apply()
        }
    }

    /** サービスが切断された */
    fun onUnbound(context: Context, reason: String) {
        runCatching {
            prefs(context).edit()
                .putLong(KEY_UNBOUND_AT, System.currentTimeMillis())
                .putString(KEY_UNBOUND_REASON, reason)
                .apply()
        }
    }

    /** 前面アプリのイベントを受信した（同じパッケージの連続記録は省く） */
    fun onEvent(context: Context, packageName: String) {
        runCatching {
            val p = prefs(context)
            if (p.getString(KEY_LAST_EVENT_PACKAGE, "") == packageName) return@runCatching
            p.edit()
                .putString(KEY_LAST_EVENT_PACKAGE, packageName)
                .putLong(KEY_LAST_EVENT_AT, System.currentTimeMillis())
                .apply()
        }
    }

    /**
     * 例外を記録する。
     * ここで記録した内容が設定画面の「診断」に表示される。
     */
    fun onError(context: Context, where: String, throwable: Throwable) {
        runCatching {
            val detail = throwable.stackTraceToString()
                .lineSequence()
                .take(ERROR_STACK_LINES)
                .joinToString("\n")
            prefs(context).edit()
                .putString(KEY_LAST_ERROR, "$where: ${throwable.javaClass.simpleName}: ${throwable.message}\n$detail")
                .putLong(KEY_LAST_ERROR_AT, System.currentTimeMillis())
                .apply()
        }
    }

    fun snapshot(context: Context): Snapshot = runCatching {
        val p = prefs(context)
        Snapshot(
            connectedAt = p.getLong(KEY_CONNECTED_AT, 0L),
            connectCount = p.getInt(KEY_CONNECT_COUNT, 0),
            unboundAt = p.getLong(KEY_UNBOUND_AT, 0L),
            unboundReason = p.getString(KEY_UNBOUND_REASON, "").orEmpty(),
            lastEventPackage = p.getString(KEY_LAST_EVENT_PACKAGE, "").orEmpty(),
            lastEventAt = p.getLong(KEY_LAST_EVENT_AT, 0L),
            lastError = p.getString(KEY_LAST_ERROR, "").orEmpty(),
            lastErrorAt = p.getLong(KEY_LAST_ERROR_AT, 0L)
        )
    }.getOrDefault(Snapshot())

    /** 診断カードに出す文字列 */
    fun summary(context: Context, serviceState: FocusServiceState): String {
        val s = snapshot(context)
        return buildString {
            appendLine("アクセシビリティ: ${serviceState.label}")
            appendLine("接続回数: ${s.connectCount} 回")
            appendLine("最終接続: ${formatTime(s.connectedAt)}")
            if (s.unboundAt > 0L) {
                appendLine("最終切断: ${formatTime(s.unboundAt)}${if (s.unboundReason.isBlank()) "" else " (${s.unboundReason})"}")
            }
            appendLine("最終イベント: ${formatTime(s.lastEventAt)}")
            if (s.lastEventPackage.isNotBlank()) {
                appendLine("最終前面アプリ: ${s.lastEventPackage}")
            }
            if (s.lastErrorAt > 0L) {
                appendLine("最終エラー: ${formatTime(s.lastErrorAt)}")
                appendLine(s.lastError)
            } else {
                append("エラー記録: なし")
            }
        }.trim()
    }

    fun formatTime(millis: Long): String {
        if (millis <= 0L) return "—"
        return runCatching {
            SimpleDateFormat("MM/dd HH:mm:ss", Locale.JAPAN).format(Date(millis))
        }.getOrDefault("—")
    }

    private const val ERROR_STACK_LINES = 12

    data class Snapshot(
        val connectedAt: Long = 0L,
        val connectCount: Int = 0,
        val unboundAt: Long = 0L,
        val unboundReason: String = "",
        val lastEventPackage: String = "",
        val lastEventAt: Long = 0L,
        val lastError: String = "",
        val lastErrorAt: Long = 0L
    )
}
