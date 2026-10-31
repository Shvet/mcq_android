package com.onegoal.mcq.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = McqColors.Primary,
    onPrimary = Color.White,
    primaryContainer = McqColors.PrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = McqColors.PrimaryDark,
    onSecondary = Color.White,
    background = Color.White,
    surface = Color.White,
    onSurface = Color(0xFF212121),
)

@Composable
fun McqTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, typography = Typography(), content = content)
}
