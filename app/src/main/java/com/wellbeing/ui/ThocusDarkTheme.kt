package com.wellbeing.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)
val DarkPrimary = Color(0xFFBB86FC)
val DarkOnPrimary = Color(0xFF25004A)
val DarkPrimaryContainer = Color(0xFF381E72)
val DarkOnPrimaryContainer = Color(0xFFEADDFF)
val DarkSecondary = Color(0xFFCE93D8)
val DarkError = Color(0xFFCF6679)
val DarkOnSurface = Color(0xFFE6E1E5)
val DarkOnSurfaceVariant = Color(0xFFCAC4D0)

private val ThocusDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = DarkError
)

@Composable
fun ThocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ThocusDarkColorScheme,
        content = content
    )
}

@Composable
fun WellbeingTheme(content: @Composable () -> Unit) {
    ThocusTheme(content = content)
}
