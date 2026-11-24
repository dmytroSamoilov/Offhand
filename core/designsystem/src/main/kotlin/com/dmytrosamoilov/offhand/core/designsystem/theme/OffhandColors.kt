package com.dmytrosamoilov.offhand.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class OffhandColors(
    val ground: Color,
    val surface: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val hairline: Color,
    val border: Color,
    val wave: Color,
    val well: Color,
    val wellSelected: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val danger: Color,
    val record: Color,
    val gold: Color,
    val onGold: Color,
    val onInk: Color,
    val scrim: Color,
    val isDark: Boolean,
)

internal val LocalOffhandColors = staticCompositionLocalOf { LightQuietInk }
