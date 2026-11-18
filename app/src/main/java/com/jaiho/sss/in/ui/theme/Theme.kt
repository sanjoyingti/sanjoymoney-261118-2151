=== ABOUT: The dark look of the whole app, fixed to green on near-black ===
package com.jaiho.sss.in.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

private val LightScheme = lightColorScheme(
    primary = AccentDim,
    onPrimary = TextMain,
    primaryContainer = Accent,
    onPrimaryContainer = Bg,
    background = Color0(),
    onBackground = Bg,
    surface = Color1(),
    onSurface = Bg,
    surfaceVariant = Color2(),
    onSurfaceVariant = Bg,
    outline = Outline,
    error = Danger,
    onError = Bg
)

private fun Color0() = Accent
private fun Color1() = androidx.compose.ui.graphics.Color(0xFFF3F7F5)
private fun Color2() = androidx.compose.ui.graphics.Color(0xFFE2EBE6)

@Composable
fun SanjoyTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography(),
        content = content
    )
}
