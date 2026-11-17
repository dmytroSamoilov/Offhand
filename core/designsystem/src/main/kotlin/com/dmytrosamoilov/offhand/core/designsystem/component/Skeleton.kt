package com.dmytrosamoilov.offhand.core.designsystem.component

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandMotion
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    lineWidths: List<Float> = DEFAULT_LINE_WIDTHS,
) {
    val brush = if (isReducedMotion()) SolidColor(OffhandTheme.colors.hairline) else sweepBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(LINE_SPACING),
    ) {
        lineWidths.forEach { fraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(LINE_HEIGHT)
                    .clip(CircleShape)
                    .background(brush),
            )
        }
    }
}

@Composable
private fun isReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

@Composable
private fun sweepBrush(): Brush {
    val colors = OffhandTheme.colors
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shift by transition.animateFloat(
        initialValue = -SWEEP_WIDTH_PX,
        targetValue = SWEEP_TRAVEL_PX,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = OffhandMotion.SKELETON_SWEEP_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeleton-shift",
    )
    return Brush.linearGradient(
        colors = listOf(colors.hairline, colors.well, colors.hairline),
        start = Offset(shift, 0f),
        end = Offset(shift + SWEEP_WIDTH_PX, 0f),
    )
}

private val DEFAULT_LINE_WIDTHS = listOf(0.92f, 1f, 0.64f)
private val LINE_HEIGHT = 10.dp
private val LINE_SPACING = 10.dp
private const val SWEEP_WIDTH_PX = 600f
private const val SWEEP_TRAVEL_PX = 1800f
