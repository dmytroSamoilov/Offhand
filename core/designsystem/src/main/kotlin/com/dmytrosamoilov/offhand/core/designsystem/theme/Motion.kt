package com.dmytrosamoilov.offhand.core.designsystem.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween

object OffhandMotion {
    const val STATE_MS = 200
    const val SHEET_MS = 300
    const val SKELETON_SWEEP_MS = 1400

    fun <T> state(): FiniteAnimationSpec<T> = tween(STATE_MS, easing = LinearOutSlowInEasing)

    fun <T> sheet(): FiniteAnimationSpec<T> = tween(SHEET_MS, easing = LinearOutSlowInEasing)
}
