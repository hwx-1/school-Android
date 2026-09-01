package com.example.schoolandorid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF2E6BE6)
private val PageBg = Color(0xFFF7F8FA)
private val CardBg = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)
private val Danger = Color(0xFFE5484D)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    secondary = TextSecondary,
    background = PageBg,
    onBackground = TextPrimary,
    surface = CardBg,
    onSurface = TextPrimary,
    surfaceVariant = PageBg,
    onSurfaceVariant = TextSecondary,
    error = Danger,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6E9BEF),
    onPrimary = Color.White,
    error = Danger,
)

/** 沈大社区主题：浅色以设计基线色值为准（对齐鸿蒙端 AppColors）。 */
@Composable
fun SchoolAndoridTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
