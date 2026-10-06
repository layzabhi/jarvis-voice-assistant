package com.example.jarvis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    secondary = TealSpeaking,
    onSecondary = Color.White,
    background = DarkBackground,
    surface = CardBackground,
    onBackground = WhiteText,
    onSurface = WhiteText
)

@Composable
fun JarvisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
