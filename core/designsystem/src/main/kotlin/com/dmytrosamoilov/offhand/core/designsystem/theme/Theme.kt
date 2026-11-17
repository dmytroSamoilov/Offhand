package com.dmytrosamoilov.offhand.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

object OffhandTheme {
    val colors: OffhandColors
        @Composable @ReadOnlyComposable get() = LocalOffhandColors.current

    val type: OffhandTextStyles
        @Composable @ReadOnlyComposable get() = LocalOffhandTextStyles.current
}

@Composable
fun OffhandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val light = if (dynamicColor) LightQuietInk.withWallpaperAccent(dynamicLightColorScheme(context)) else LightQuietInk
    val dark = if (dynamicColor) DarkQuietInk.withWallpaperAccent(dynamicDarkColorScheme(context)) else DarkQuietInk
    CompositionLocalProvider(LocalDarkPalette provides dark) {
        QuietInkScope(colors = if (darkTheme) dark else light, content = content)
    }
}

@Composable
fun OffhandDarkContent(content: @Composable () -> Unit) {
    QuietInkScope(colors = LocalDarkPalette.current, content = content)
}

@Composable
private fun QuietInkScope(colors: OffhandColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalOffhandColors provides colors,
        LocalOffhandTextStyles provides QuietInkTextStyles,
    ) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = OffhandTypography,
            shapes = OffhandShapes,
            content = content,
        )
    }
}

private val LocalDarkPalette = staticCompositionLocalOf { DarkQuietInk }

private fun OffhandColors.withWallpaperAccent(scheme: ColorScheme) = copy(
    accent = scheme.primary,
    onAccent = scheme.onPrimary,
    accentSoft = scheme.primaryContainer,
)

private fun OffhandColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = ink,
        onPrimary = onInk,
        primaryContainer = accentSoft,
        onPrimaryContainer = ink,
        inversePrimary = accent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = well,
        onSecondaryContainer = ink,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = accentSoft,
        onTertiaryContainer = ink,
        background = ground,
        onBackground = ink,
        surface = ground,
        onSurface = ink,
        surfaceVariant = well,
        onSurfaceVariant = ink2,
        surfaceTint = ground,
        inverseSurface = ink,
        inverseOnSurface = onInk,
        error = danger,
        onError = onInk,
        errorContainer = well,
        onErrorContainer = ink,
        outline = border,
        outlineVariant = hairline,
        scrim = scrim,
        surfaceBright = surface,
        surfaceDim = ground,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
    )
}
