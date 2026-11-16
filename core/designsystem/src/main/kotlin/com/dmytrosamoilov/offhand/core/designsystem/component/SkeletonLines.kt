package com.dmytrosamoilov.offhand.core.designsystem.component

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonLines(
    modifier: Modifier = Modifier,
    lineWidths: List<Float> = DEFAULT_LINE_WIDTHS,
) {
    val brush = shimmerBrush()
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
private fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = BASE_ALPHA)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = HIGHLIGHT_ALPHA)
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shift by transition.animateFloat(
        initialValue = -SHIMMER_WIDTH_PX,
        targetValue = SHIMMER_TRAVEL_PX,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeleton-shift",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(shift, 0f),
        end = Offset(shift + SHIMMER_WIDTH_PX, 0f),
    )
}

private val DEFAULT_LINE_WIDTHS = listOf(0.45f, 1f, 0.94f, 0.98f, 0.7f, 1f, 0.88f, 0.5f)
private val LINE_HEIGHT = 14.dp
private val LINE_SPACING = 12.dp
private const val BASE_ALPHA = 0.08f
private const val HIGHLIGHT_ALPHA = 0.18f
private const val SHIMMER_WIDTH_PX = 600f
private const val SHIMMER_TRAVEL_PX = 1800f
private const val SHIMMER_DURATION_MS = 1400
