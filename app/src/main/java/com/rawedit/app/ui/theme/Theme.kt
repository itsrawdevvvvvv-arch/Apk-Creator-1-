package com.rawedit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

data class RawMotionConfig(
    val reducedMotion: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val isLowRamDevice: Boolean = false
)

val LocalRawMotion = staticCompositionLocalOf { RawMotionConfig() }

private val RawColorScheme = darkColorScheme(
    primary = RawColors.TextPrimary,
    onPrimary = RawColors.Background,
    secondary = RawColors.TextSecondary,
    onSecondary = RawColors.Background,
    background = RawColors.Background,
    onBackground = RawColors.TextPrimary,
    surface = RawColors.Surface,
    onSurface = RawColors.TextPrimary,
    surfaceVariant = RawColors.SurfaceElevated,
    onSurfaceVariant = RawColors.TextSecondary,
    outline = RawColors.Divider,
    error = RawColors.ErrorCrimson,
    onError = RawColors.TextPrimary
)

@Composable
fun RawEditTheme(
    motionConfig: RawMotionConfig = RawMotionConfig(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalRawMotion provides motionConfig) {
        MaterialTheme(
            colorScheme = RawColorScheme,
            typography = RawTypography,
            content = content
        )
    }
}
