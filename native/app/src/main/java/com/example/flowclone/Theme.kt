package com.example.flowclone

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF000000)
val Surface = Color(0xFF2C2D30)
val Surface2 = Color(0xFF171819)
val Surface3 = Color(0xFF3A3B3E)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF9AA0A6)
val Accent = Color(0xFFFF5722)

private val scheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    secondary = TextSecondary,
    onSecondary = Color.Black,
    error = Color(0xFFFF8A80),
)

@Composable
fun FlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
