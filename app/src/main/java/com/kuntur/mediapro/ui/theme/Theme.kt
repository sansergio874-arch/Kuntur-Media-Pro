package com.kuntur.mediapro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF0B1C2C)
private val Gold = Color(0xFFD4A017)
private val SurfaceDark = Color(0xFF152536)

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Navy,
    secondary = Color(0xFF4A90A4),
    background = Navy,
    surface = SurfaceDark,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun KunturMediaProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
