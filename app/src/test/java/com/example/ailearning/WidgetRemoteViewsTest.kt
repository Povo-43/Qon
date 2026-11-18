package com.example.ailearning

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.widget.RemoteViews
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ウィジェットレイアウトが RemoteViews として正しく適用できることを検証する。
 * ランチャー側の適用失敗（「ウィジェットを読み込めません」）を防ぐための回帰テスト。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class WidgetRemoteViewsTest {

    @Test
    fun widgetLayoutInflates() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val views = RemoteViews(context.packageName, R.layout.widget_quiz)
        val host = FrameLayout(context)

        val applied = views.apply(context, host)
        assertNotNull(applied)
        assertNotNull(applied.findViewById(R.id.tv_question))
        assertNotNull(applied.findViewById(R.id.widget_bg))
        assertNotNull(applied.findViewById(R.id.option_row_a))
    }

    @Test
    fun rendererOperationsApply() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val views = RemoteViews(context.packageName, R.layout.widget_quiz)

        // WidgetRenderer が行うのと同じ操作一式
        views.setInt(R.id.widget_root, "setBackgroundColor", 0xFF112233.toInt())
        views.setViewVisibility(R.id.widget_bg, View.VISIBLE)
        views.setImageViewResource(R.id.widget_bg, R.drawable.widget_bg_indigo)
        views.setTextViewText(R.id.tv_badge, "Qon  ·  問題 1")
        views.setTextColor(R.id.tv_badge, 0xFFC7D2FE.toInt())
        views.setTextViewText(R.id.tv_question, "テスト問題")
        views.setTextColor(R.id.tv_question, 0xFFFFFFFF.toInt())
        views.setViewVisibility(R.id.options_container, View.VISIBLE)
        views.setInt(R.id.option_row_a, "setBackgroundResource", R.drawable.widget_option_row)
        views.setInt(R.id.option_bar_a, "setBackgroundColor", 0xFFFF0000.toInt())
        views.setTextViewText(R.id.option_text_a, "A. 選択肢")
        views.setViewVisibility(R.id.btn_next, View.VISIBLE)

        val host = FrameLayout(context)
        val applied = views.apply(context, host)

        assertEquals("テスト問題", applied.findViewById<TextView>(R.id.tv_question).text.toString())
        assertEquals(
            View.VISIBLE,
            applied.findViewById<View>(R.id.options_container).visibility
        )
    }

    @Test
    fun allPresetBackgroundsResolve() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val ids = listOf(
            R.drawable.widget_bg_indigo,
            R.drawable.widget_bg_midnight,
            R.drawable.widget_bg_ocean,
            R.drawable.widget_bg_forest,
            R.drawable.widget_bg_sunset,
            R.drawable.widget_bg_rose,
            R.drawable.widget_bg_grape,
            R.drawable.widget_bg_graphite,
            R.drawable.widget_bg_daylight,
            R.drawable.widget_bg_paper
        )
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_quiz)
            views.setImageViewResource(R.id.widget_bg, id)
            val host = FrameLayout(context)
            views.apply(context, host)
        }
    }
}
