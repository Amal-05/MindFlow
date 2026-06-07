package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    tertiary = AccentColor,
    background = BackgroundColor,
    surface = SurfaceColor,
    surfaceVariant = CardColor,
    onPrimary = Color.White,
    onSecondary = Color(0xFF0F1117),
    onTertiary = Color(0xFF0F1117),
    onBackground = TextPrimaryColor,
    onSurface = TextPrimaryColor,
    onSurfaceVariant = TextSecondaryColor,
    outline = TextMutedColor,
    error = DangerColor
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    tertiary = AccentColor,
    background = Color(0xFFFAFBFD),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F3F9),
    onPrimary = Color.White,
    onSecondary = Color(0xFF0F1117),
    onBackground = Color(0xFF0F1117),
    onSurface = Color(0xFF0F1117),
    onSurfaceVariant = Color(0xFF4A5568),
    outline = Color(0xFF718096),
    error = DangerColor
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We make it premium-dark-first as requested
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
