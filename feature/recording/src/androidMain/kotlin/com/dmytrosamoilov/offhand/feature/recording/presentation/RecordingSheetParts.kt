package com.dmytrosamoilov.offhand.feature.recording.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.recording.R

@Composable
internal fun StatusDot(color: Color, size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
internal fun IconGlyph(icon: ImageVector, size: Dp) {
    Icon(imageVector = icon, contentDescription = null, tint = OffhandTheme.colors.ink2, modifier = Modifier.size(size))
}

@Composable
internal fun IconLine(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        IconGlyph(icon = icon, size = 14.dp)
        Text(text = text, style = OffhandTheme.type.caption, color = OffhandTheme.colors.ink2)
    }
}

@Composable
internal fun RecordingWaveform(levels: List<Float>, modifier: Modifier = Modifier) {
    val color = OffhandTheme.colors.ink2
    Canvas(modifier = modifier.height(WAVEFORM_HEIGHT)) {
        val step = BAR_STEP.toPx()
        val visible = levels.takeLast((size.width / step).toInt())
        val startX = size.width - visible.size * step + step / 2
        visible.forEachIndexed { index, level ->
            drawBar(x = startX + index * step, level = level, color = color)
        }
    }
}

private fun DrawScope.drawBar(x: Float, level: Float, color: Color) {
    val stroke = BAR_WIDTH.toPx()
    val half = maxOf(level * (size.height - stroke), MIN_BAR.toPx()) / 2
    drawLine(
        color = color,
        start = Offset(x, size.height / 2 - half),
        end = Offset(x, size.height / 2 + half),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

@Composable
internal fun ChunkStates(chunks: List<ChunkUi>) {
    Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        chunks.takeLast(MAX_CHUNKS_SHOWN).forEach { chunk ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusDot(color = chunk.state.color(), size = 6.dp)
                Text(
                    text = stringResource(R.string.recording_chunk_label, chunk.id),
                    style = OffhandTheme.type.meta,
                    color = OffhandTheme.colors.ink2,
                )
            }
        }
    }
}

@Composable
private fun ChunkStateUi.color(): Color = when (this) {
    ChunkStateUi.QUEUED -> OffhandTheme.colors.wave
    ChunkStateUi.TRANSCRIBING -> OffhandTheme.colors.accent
    ChunkStateUi.DONE -> OffhandTheme.colors.ink
    ChunkStateUi.FAILED -> OffhandTheme.colors.danger
}

private val WAVEFORM_HEIGHT = 120.dp
private val BAR_WIDTH = 3.dp
private val BAR_STEP = 8.dp
private val MIN_BAR = 8.dp
private const val MAX_CHUNKS_SHOWN = 6
