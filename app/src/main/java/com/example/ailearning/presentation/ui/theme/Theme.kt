package com.example.ailearning.presentation.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFAAC1FF),
    onPrimary = Color(0xFF0A1F4C),
    primaryContainer = Color(0xFF1D3A6B),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFFCCBEFF),
    onSecondary = Color(0xFF1A0E40),
    secondaryContainer = Color(0xFF3F2DB1),
    onSecondaryContainer = Color(0xFFE8E0FF),
    tertiary = Color(0xFF80E5E0),
    onTertiary = Color(0xFF002220),
    tertiaryContainer = Color(0xFF004E4B),
    onTertiaryContainer = Color(0xFF9CF0EB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF3B0C0C),
    errorContainer = Color(0xFF5A1B1B),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101415),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF101415),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC3C7CE),
    outline = Color(0xFF8E9199),
    outlineVariant = Color(0xFF44474E),
    inverseSurface = Color(0xFFE2E2E9),
    inverseOnSurface = Color(0xFF2E3033),
    inversePrimary = Color(0xFF3B5BDB),
    surfaceTint = Color(0xFFAAC1FF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3B5BDB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001845),
    secondary = Color(0xFF6C4EDC),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E0FF),
    onSecondaryContainer = Color(0xFF270063),
    tertiary = Color(0xFF006C68),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9CF0EB),
    onTertiaryContainer = Color(0xFF00211F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF191C1D),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CE),
    inverseSurface = Color(0xFF2E3033),
    inverseOnSurface = Color(0xFFF0F0F7),
    inversePrimary = Color(0xFFAAC1FF),
    surfaceTint = Color(0xFF3B5BDB)
)

@Composable
fun AiLearningTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    val context = LocalContext.current
    
    SideEffect {
        (context as? androidx.appcompat.app.AppCompatActivity)?.window?.apply {
            statusBarColor = colorScheme.background.value.toInt()
            navigationBarColor = colorScheme.background.value.toInt()
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
