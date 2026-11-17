package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme

@Composable
fun Player(
    isPlaying: Boolean,
    progress: Float,
    elapsedLabel: String,
    playDescription: String,
    pauseDescription: String,
    seekDescription: String,
    waveSeed: Long,
    onToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    waveform: List<Float> = emptyList(),
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RoundIconButton(
            icon = if (isPlaying) OffhandIcons.Pause else OffhandIcons.Play,
            contentDescription = if (isPlaying) pauseDescription else playDescription,
            onClick = onToggle,
            size = 48.dp,
            filled = true,
            enabled = enabled,
        )
        Waveform(
            progress = progress,
            waveSeed = waveSeed,
            waveform = waveform,
            seekDescription = seekDescription,
            onSeek = onSeek,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        Text(text = elapsedLabel, style = OffhandTheme.type.number, color = OffhandTheme.colors.ink2)
    }
}

@Composable
private fun Waveform(
    progress: Float,
    waveSeed: Long,
    waveform: List<Float>,
    seekDescription: String,
    onSeek: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = OffhandTheme.colors
    val bars = remember(waveSeed, waveform) { waveform.toBarHeights().ifEmpty { waveBars(waveSeed, PLAYER_BARS) } }
    val currentOnSeek by rememberUpdatedState(onSeek)
    val seekModifier = if (enabled) {
        Modifier
            .pointerInput(Unit) { detectTapGestures { currentOnSeek((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> currentOnSeek((change.position.x / size.width).coerceIn(0f, 1f)) }
            }
    } else {
        Modifier
    }
    Canvas(
        modifier = modifier
            .height(32.dp)
            .then(seekModifier)
            .semantics {
                contentDescription = seekDescription
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                if (enabled) {
                    setProgress { target ->
                        currentOnSeek(target.coerceIn(0f, 1f))
                        true
                    }
                }
            },
    ) {
        val step = size.width / bars.size
        val stroke = 2.5.dp.toPx()
        bars.forEachIndexed { index, height ->
            val x = step * index + step / 2
            val half = (size.height - stroke) * height / 2
            val played = x / size.width <= progress
            drawLine(
                if (played) colors.ink else colors.wave,
                Offset(x, size.height / 2 - half),
                Offset(x, size.height / 2 + half),
                stroke,
                StrokeCap.Round,
            )
        }
    }
}

private const val PLAYER_BARS = 37
