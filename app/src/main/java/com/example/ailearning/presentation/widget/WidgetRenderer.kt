package com.example.ailearning.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.ailearning.R
import com.example.ailearning.domain.model.Question
import com.example.ailearning.domain.model.WidgetColors
import com.example.ailearning.domain.model.WidgetConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ウィジェットの描画を一元管理するクラス。
 * WidgetConfig の配色設定を RemoteViews に反映する。
 *
 * RemoteViews で安全な操作のみを使う方針:
 * - 背景はプリセット色ごとに事前生成したシェイプdrawable（setImageViewResource）
 * - カスタム色は root の setBackgroundColor（@RemotableViewMethod 付きの標準操作）
 * - setColorFilter などの反射呼び出しは環境により失敗するため使わない
 */
object WidgetRenderer {

    /** ウィジェットに表示する状態 */
    data class WidgetState(
        val question: Question? = null,
        val showResult: Boolean = false,
        val isCorrect: Boolean = false,
        val selectedAnswer: Int = -1,
        /** 「問題 N」表示用。0 なら非表示 */
        val questionNumber: Int = 0,
        /** 問題が無い場合などの案内文（null なら既定文） */
        val emptyMessage: String? = null
    )

    fun render(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        config: WidgetConfig?,
        state: WidgetState
    ) {
        val colors = (config ?: WidgetConfig(widgetId = widgetId, learningSetId = "")).resolvedColors()
        val views = RemoteViews(context.packageName, R.layout.widget_quiz)

        applyBackground(views, colors, config)
        applyHeader(views, colors, state, config)

        val question = state.question
        if (question == null) {
            applyEmpty(views, colors, state.emptyMessage)
        } else {
            applyQuestion(context, views, colors, widgetId, config, state, question)
        }

        applyNextButton(context, views, colors, widgetId, config, state)
        applyOpenAppClick(context, views, widgetId)

        try {
            manager.updateAppWidget(widgetId, views)
        } catch (e: Exception) {
            // 最悪でも空白を避け、既定レイアウトの最低限の表示に戻す
            runCatching {
                val fallback = RemoteViews(context.packageName, R.layout.widget_quiz)
                fallback.setTextViewText(R.id.tv_question, "ウィジェットの更新に失敗しました\nアプリを開いて再試行してください")
                manager.updateAppWidget(widgetId, fallback)
            }
        }
    }

    /** 未設定・未構成のウィジェット用のプレースホルダを描く。 */
    fun renderPlaceholder(context: Context, manager: AppWidgetManager, widgetId: Int) {
        render(
            context, manager, widgetId,
            config = null,
            state = WidgetState(
                question = null,
                emptyMessage = "読み込み中…"
            )
        )
    }

    // ---------------------------------------------------------------- header

    private fun applyHeader(
        views: RemoteViews,
        colors: WidgetColors,
        state: WidgetState,
        config: WidgetConfig?
    ) {
        val badge = buildString {
            append("Qon")
            if (state.questionNumber > 0) append("  ·  問題 ").append(state.questionNumber)
        }
        views.setTextViewText(R.id.tv_badge, badge)
        views.setTextColor(R.id.tv_badge, colors.accent)

        val time = config?.lastUpdatedAt?.takeIf { it > 0 }?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
        }
        views.setTextViewText(R.id.tv_updated, time?.let { "更新 $it" } ?: "")
        views.setTextColor(R.id.tv_updated, colors.secondaryText)

        views.setTextColor(R.id.tv_hint, colors.secondaryText)
    }

    // -------------------------------------------------------------- question

    private fun applyQuestion(
        context: Context,
        views: RemoteViews,
        colors: WidgetColors,
        widgetId: Int,
        config: WidgetConfig?,
        state: WidgetState,
        question: Question
    ) {
        views.setTextViewText(R.id.tv_question, question.questionText)
        views.setTextColor(R.id.tv_question, colors.text)

        views.setViewVisibility(R.id.options_container, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.tv_hint, android.view.View.VISIBLE)

        val showResult = state.showResult
        if (showResult) {
            views.setViewVisibility(R.id.result_container, android.view.View.VISIBLE)
            val resultText = if (state.isCorrect) "正解！" else "不正解…"
            val resultColor = if (state.isCorrect) colors.correct else colors.incorrect
            views.setTextViewText(R.id.tv_result, resultText)
            views.setTextColor(R.id.tv_result, resultColor)
            if (config?.showExplanation != false) {
                views.setViewVisibility(R.id.tv_explanation, android.view.View.VISIBLE)
                views.setTextViewText(R.id.tv_explanation, question.explanation.ifBlank { "解説はありません" })
                views.setTextColor(R.id.tv_explanation, colors.secondaryText)
            } else {
                views.setViewVisibility(R.id.tv_explanation, android.view.View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.result_container, android.view.View.GONE)
        }

        val rowIds = listOf(
            R.id.option_row_a, R.id.option_row_b, R.id.option_row_c, R.id.option_row_d
        )
        val barIds = listOf(
            R.id.option_bar_a, R.id.option_bar_b, R.id.option_bar_c, R.id.option_bar_d
        )
        val textIds = listOf(
            R.id.option_text_a, R.id.option_text_b, R.id.option_text_c, R.id.option_text_d
        )

        repeat(4) { index ->
            val option = question.getOptions().getOrNull(index).orEmpty()
            views.setTextViewText(textIds[index], "${('A' + index)}. $option")
            views.setTextColor(textIds[index], colors.text)

            val isCorrectOption = index == question.correctAnswer
            val isSelected = index == state.selectedAnswer
            val rowBackground = when {
                showResult && isCorrectOption -> R.drawable.widget_option_correct
                showResult && isSelected -> R.drawable.widget_option_incorrect
                colors.isLight -> R.drawable.widget_option_row_light
                else -> R.drawable.widget_option_row
            }
            views.setInt(rowIds[index], "setBackgroundResource", rowBackground)

            val barColor = when {
                showResult && isCorrectOption -> colors.correct
                showResult && isSelected -> colors.incorrect
                else -> colors.accent
            }
            views.setInt(barIds[index], "setBackgroundColor", barColor)

            // 回答用クリック（結果表示中は無効化）
            if (!showResult) {
                views.setOnClickPendingIntent(
                    rowIds[index],
                    answerPendingIntent(context, widgetId, index)
                )
            }
        }
    }

    private fun applyEmpty(views: RemoteViews, colors: WidgetColors, message: String?) {
        views.setTextViewText(
            R.id.tv_question,
            message ?: "問題がありません\nアプリで問題を生成してください"
        )
        views.setTextColor(R.id.tv_question, colors.text)
        views.setViewVisibility(R.id.options_container, android.view.View.GONE)
        views.setViewVisibility(R.id.result_container, android.view.View.GONE)
        views.setViewVisibility(R.id.tv_hint, android.view.View.GONE)
        views.setViewVisibility(R.id.btn_next, android.view.View.GONE)
    }

    private fun applyNextButton(
        context: Context,
        views: RemoteViews,
        colors: WidgetColors,
        widgetId: Int,
        config: WidgetConfig?,
        state: WidgetState
    ) {
        val showNext = state.question != null && (!state.showResult || config?.autoNext != true)
        views.setViewVisibility(R.id.btn_next, if (showNext) android.view.View.VISIBLE else android.view.View.GONE)
        if (showNext) {
            views.setTextColor(R.id.btn_next, colors.accent)
            views.setOnClickPendingIntent(R.id.btn_next, nextPendingIntent(context, widgetId))
        }
    }

    /** 問題文をタップしたらアプリを開く。 */
    private fun applyOpenAppClick(context: Context, views: RemoteViews, widgetId: Int) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val pendingIntent = PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.tv_question, pendingIntent)
    }

    // ------------------------------------------------------------- background

    /**
     * 背景。プリセット色は事前生成drawable、カスタム色は setBackgroundColor。
     * どちらも RemoteViews で最も安全な操作のみを使用する。
     */
    private fun applyBackground(views: RemoteViews, colors: WidgetColors, config: WidgetConfig?) {
        val customBackground = config?.backgroundColor ?: 0
        if (customBackground != 0) {
            // カスタム色: root に直接色を付ける（角丸は Android 12+ ならシステムが適用）
            views.setInt(R.id.widget_root, "setBackgroundColor", customBackground)
            views.setViewVisibility(R.id.widget_bg, android.view.View.GONE)
        } else {
            views.setInt(R.id.widget_root, "setBackgroundColor", Color.TRANSPARENT)
            views.setViewVisibility(R.id.widget_bg, android.view.View.VISIBLE)
            views.setImageViewResource(R.id.widget_bg, backgroundDrawableFor(colors.id))
        }
    }

    private fun backgroundDrawableFor(themeId: String): Int = when (themeId) {
        "midnight" -> R.drawable.widget_bg_midnight
        "ocean" -> R.drawable.widget_bg_ocean
        "forest" -> R.drawable.widget_bg_forest
        "sunset" -> R.drawable.widget_bg_sunset
        "rose" -> R.drawable.widget_bg_rose
        "grape" -> R.drawable.widget_bg_grape
        "graphite" -> R.drawable.widget_bg_graphite
        "daylight" -> R.drawable.widget_bg_daylight
        "paper" -> R.drawable.widget_bg_paper
        else -> R.drawable.widget_bg_indigo
    }

    // ----------------------------------------------------------- intents

    internal fun answerPendingIntent(context: Context, widgetId: Int, answerIndex: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            widgetId * 4 + answerIndex,
            Intent(ACTION_ANSWER).setClass(context, WidgetAnswerReceiver::class.java)
                .putExtra("widget_id", widgetId)
                .putExtra("action", "answer")
                .putExtra("selected_answer", answerIndex),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    internal fun nextPendingIntent(context: Context, widgetId: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            widgetId,
            Intent(ACTION_NEXT).setClass(context, WidgetAnswerReceiver::class.java)
                .putExtra("widget_id", widgetId)
                .putExtra("action", "next"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private const val ACTION_ANSWER = "com.example.ailearning.WIDGET_ANSWER"
    private const val ACTION_NEXT = "com.example.ailearning.WIDGET_NEXT"
}
