package com.example.ailearning.domain.model

import androidx.core.graphics.ColorUtils

/**
 * ウィジェットの配色定義。
 * 背景色を基準に、テキスト色や選択肢の表面色を自動導出する。
 */
data class WidgetColors(
    val id: String,
    val label: String,
    val background: Int,
    val accent: Int
) {
    /** 背景が明るいか（文字色の自動判定に使う） */
    val isLight: Boolean
        get() = ColorUtils.calculateLuminance(background) > 0.5

    /** メインテキスト色（背景から自動コントラスト） */
    val text: Int
        get() = if (isLight) TEXT_DARK else TEXT_LIGHT

    /** 補助テキスト色 */
    val secondaryText: Int
        get() = if (isLight) TEXT_DARK_SECONDARY else TEXT_LIGHT_SECONDARY

    /** 選択肢の背景色 */
    val optionBackground: Int
        get() = if (isLight) OPTION_LIGHT else OPTION_DARK

    /** 正解ハイライト */
    val correct: Int
        get() = CORRECT

    /** 不正解ハイライト */
    val incorrect: Int
        get() = INCORRECT

    /** 背景グラデーションのもう片方の色 */
    val backgroundGradientEnd: Int
        get() {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(background, hsl)
            hsl[2] = if (isLight) (hsl[2] - 0.06f).coerceAtLeast(0f) else (hsl[2] + 0.09f).coerceAtMost(1f)
            return ColorUtils.HSLToColor(hsl)
        }

    fun withOverrides(backgroundOverride: Int, accentOverride: Int): WidgetColors {
        var colors = this
        if (backgroundOverride != 0) {
            colors = colors.copy(background = backgroundOverride)
        }
        if (accentOverride != 0) {
            colors = colors.copy(accent = accentOverride)
        }
        return colors
    }

    companion object {
        const val TEXT_LIGHT = 0xFFF8FAFC.toInt()
        const val TEXT_LIGHT_SECONDARY = 0xFFC7D0DE.toInt()
        const val TEXT_DARK = 0xFF14161F.toInt()
        const val TEXT_DARK_SECONDARY = 0xFF5B6073.toInt()
        const val OPTION_LIGHT = 0xFFF1F3F9.toInt()
        const val OPTION_DARK = 0x2EFFFFFF.toInt()
        const val CORRECT = 0xFF34D399.toInt()
        const val INCORRECT = 0xFFF87171.toInt()

        val PRESETS: List<WidgetColors> = listOf(
            WidgetColors("indigo", "インディゴ", 0xFF4F46E5.toInt(), 0xFFC7D2FE.toInt()),
            WidgetColors("midnight", "ミッドナイト", 0xFF0F172A.toInt(), 0xFF38BDF8.toInt()),
            WidgetColors("ocean", "オーシャン", 0xFF0E7490.toInt(), 0xFF67E8F9.toInt()),
            WidgetColors("forest", "フォレスト", 0xFF166534.toInt(), 0xFF86EFAC.toInt()),
            WidgetColors("sunset", "サンセット", 0xFFC2410C.toInt(), 0xFFFDBA74.toInt()),
            WidgetColors("rose", "ローズ", 0xFF9D174D.toInt(), 0xFFFDA4AF.toInt()),
            WidgetColors("grape", "グレープ", 0xFF6D28D9.toInt(), 0xFFDDD6FE.toInt()),
            WidgetColors("graphite", "グラファイト", 0xFF1F2937.toInt(), 0xFFA5B4FC.toInt()),
            WidgetColors("daylight", "デイライト", 0xFFF6F7FB.toInt(), 0xFF4F46E5.toInt()),
            WidgetColors("paper", "ペーパー", 0xFFEDE9E3.toInt(), 0xFF9A3412.toInt())
        )

        fun preset(id: String): WidgetColors =
            PRESETS.firstOrNull { it.id == id } ?: PRESETS.first()

        fun byId(id: String): WidgetColors? = PRESETS.firstOrNull { it.id == id }
    }
}
