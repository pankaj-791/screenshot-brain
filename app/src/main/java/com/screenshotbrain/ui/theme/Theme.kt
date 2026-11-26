package com.screenshotbrain.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.screenshotbrain.data.AccentColor

private fun lightScheme(accent: Color): ColorScheme {
    // Bright accents are darkened a little so white text on them stays readable.
    val primary = if (accent.luminance() > 0.2f) lerp(accent, Color.Black, 0.15f) else accent
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = lerp(accent, Color.White, 0.82f),
        onPrimaryContainer = lerp(accent, Color.Black, 0.65f),
        secondary = lerp(primary, Color(0xFF49454F), 0.5f),
        onSecondary = Color.White,
        secondaryContainer = lerp(accent, Color.White, 0.88f),
        onSecondaryContainer = lerp(accent, Color.Black, 0.7f),
        background = Color(0xFFFAF9FC),
        onBackground = Color(0xFF1C1B1F),
        surface = Color(0xFFFAF9FC),
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFEAE6F0),
        onSurfaceVariant = Color(0xFF49454F)
    )
}

private fun darkScheme(accent: Color): ColorScheme =
    darkColorScheme(
        primary = lerp(accent, Color.White, 0.55f),
        onPrimary = lerp(accent, Color.Black, 0.75f),
        primaryContainer = lerp(accent, Color.Black, 0.45f),
        onPrimaryContainer = lerp(accent, Color.White, 0.85f),
        secondary = lerp(accent, Color.White, 0.65f),
        onSecondary = lerp(accent, Color.Black, 0.75f),
        secondaryContainer = lerp(accent, Color.Black, 0.6f),
        onSecondaryContainer = lerp(accent, Color.White, 0.85f),
        background = Color(0xFF141318),
        onBackground = Color(0xFFE6E1E9),
        surface = Color(0xFF141318),
        onSurface = Color(0xFFE6E1E9),
        surfaceVariant = Color(0xFF2B2930),
        onSurfaceVariant = Color(0xFFCAC4D0)
    )

@Composable
fun ScreenshotBrainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AccentColor = AccentColor.Violet,
    content: @Composable () -> Unit
) {
    val scheme = remember(darkTheme, accent) {
        val base = Color(accent.argb)
        if (darkTheme) darkScheme(base) else lightScheme(base)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
