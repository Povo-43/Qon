package com.example.ailearning

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ailearning.presentation.focus.FocusDiagnostics
import com.example.ailearning.presentation.focus.FocusServiceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * 集中モードの診断記録が端末内に残り、設定画面に表示できることを検証する。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class FocusDiagnosticsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("focus_diagnostics", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun connectionIsRecorded() {
        FocusDiagnostics.onConnected(context)
        FocusDiagnostics.onConnected(context)

        val snapshot = FocusDiagnostics.snapshot(context)
        assertEquals(2, snapshot.connectCount)
        assertTrue(snapshot.connectedAt > 0L)
    }

    @Test
    fun foregroundEventIsRecordedOnce() {
        FocusDiagnostics.onEvent(context, "com.example.browser")
        FocusDiagnostics.onEvent(context, "com.example.browser")

        val snapshot = FocusDiagnostics.snapshot(context)
        assertEquals("com.example.browser", snapshot.lastEventPackage)
        assertTrue(snapshot.lastEventAt > 0L)
    }

    @Test
    fun errorIsPersistedWithStackTrace() {
        FocusDiagnostics.onError(context, "onServiceConnected", IllegalStateException("boom"))

        val snapshot = FocusDiagnostics.snapshot(context)
        assertTrue(snapshot.lastError.contains("onServiceConnected"))
        assertTrue(snapshot.lastError.contains("boom"))
        assertTrue(snapshot.lastErrorAt > 0L)
    }

    @Test
    fun summaryShowsServiceStateAndError() {
        FocusDiagnostics.onError(context, "onAccessibilityEvent", RuntimeException("crash"))

        val summary = FocusDiagnostics.summary(context, FocusServiceState.ENABLED_NOT_CONNECTED)
        assertTrue(summary.contains(FocusServiceState.ENABLED_NOT_CONNECTED.label))
        assertTrue(summary.contains("crash"))
        assertTrue(summary.contains("接続回数: 0 回"))
    }

    @Test
    fun summaryIsSafeWithoutAnyRecord() {
        val summary = FocusDiagnostics.summary(context, FocusServiceState.DISABLED)
        assertTrue(summary.contains(FocusServiceState.DISABLED.label))
        assertTrue(summary.contains("エラー記録: なし"))
    }
}
