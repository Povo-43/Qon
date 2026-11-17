package com.example.ailearning.presentation.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E2FF),
    onPrimaryContainer = Color(0xFF150B63),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0E7FF),
    onSecondaryContainer = Color(0xFF2A0B63),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF00201D),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF450A0A),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF14161F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14161F),
    surfaceVariant = Color(0xFFEDEFF7),
    onSurfaceVariant = Color(0xFF5B6073),
    outline = Color(0xFF8A90A4),
    outlineVariant = Color(0xFFDDE1EC),
    inverseSurface = Color(0xFF2C2F3A),
    inverseOnSurface = Color(0xFFF1F2F7),
    inversePrimary = Color(0xFFB9B4FF),
    surfaceTint = Color(0xFF4F46E5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9B4FF),
    onPrimary = Color(0xFF23149B),
    primaryContainer = Color(0xFF3A2FC0),
    onPrimaryContainer = Color(0xFFE4E2FF),
    secondary = Color(0xFFD2BBFF),
    onSecondary = Color(0xFF3D1D7A),
    secondaryContainer = Color(0xFF553197),
    onSecondaryContainer = Color(0xFFF0E7FF),
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF003733),
    tertiaryContainer = Color(0xFF0F766E),
    onTertiaryContainer = Color(0xFFCCFBF1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0C0E14),
    onBackground = Color(0xFFE5E7EF),
    surface = Color(0xFF13161F),
    onSurface = Color(0xFFE5E7EF),
    surfaceVariant = Color(0xFF232734),
    onSurfaceVariant = Color(0xFFAAB0C0),
    outline = Color(0xFF767C8C),
    outlineVariant = Color(0xFF353B4A),
    inverseSurface = Color(0xFFE5E7EF),
    inverseOnSurface = Color(0xFF2B2F3A),
    inversePrimary = Color(0xFF4F46E5),
    surfaceTint = Color(0xFFB9B4FF)
)

private val QonTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

private val QonShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
)

@Composable
fun AiLearningTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QonTypography,
        shapes = QonShapes,
        content = content
    )
}
