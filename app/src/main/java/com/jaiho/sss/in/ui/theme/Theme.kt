package com.jaiho.sss.in.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Bg,
    primaryContainer = AccentDim,
    onPrimaryContainer = TextMain,
    secondary = Accent,
    onSecondary = Bg,
    background = Bg,
    onBackground = TextMain,
    surface = Surface1,
    onSurface = TextMain,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim,
    outline = Outline,
    error = Danger,
    onError = Bg
)

private val LightBg = Color(0xFFF3F7F5)
private val LightSurface = Color(0xFFE2EBE6)

private val LightScheme = lightColorScheme(
    primary = AccentDim,
    onPrimary = TextMain,
    primaryContainer = Accent,
    onPrimaryContainer = Bg,
    background = LightBg,
    onBackground = Bg,
    surface = LightSurface,
    onSurface = Bg,
    surfaceVariant = LightSurface,
    onSurfaceVariant = Bg,
    outline = Outline,
    error = Danger,
    onError = Bg
)

@Composable
fun SanjoyTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography(),
        content = content
    )
}
