package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import kotlin.random.Random

sealed interface NoteRowState {
    data class Ready(val preview: AnnotatedString?) : NoteRowState
    data class Processing(val text: String, val progress: Float?) : NoteRowState
    data class Attention(val text: String) : NoteRowState
}

@Composable
fun NoteRow(
    title: AnnotatedString,
    state: NoteRowState,
    meta: String?,
    waveSeed: Long,
    modifier: Modifier = Modifier,
    waveform: List<Float> = emptyList(),
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
    showWave: Boolean = true,
    previewLines: Int = 1,
) {
    val colors = OffhandTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 14.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        leading()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = OffhandTheme.type.rowTitle,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            NoteRowBody(state, previewLines)
            meta?.let { MetaText(text = it, modifier = Modifier.padding(top = 2.dp)) }
        }
        if (showWave && state is NoteRowState.Ready) WaveGlyph(seed = waveSeed, levels = waveform)
        trailing()
    }
}

@Composable
private fun NoteRowBody(state: NoteRowState, previewLines: Int) {
    val colors = OffhandTheme.colors
    when (state) {
        is NoteRowState.Ready -> state.preview?.let {
            Text(
                text = it,
                style = OffhandTheme.type.bodySmall,
                color = colors.ink2,
                maxLines = previewLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        is NoteRowState.Processing -> {
            Text(text = state.text, style = OffhandTheme.type.bodySmall, fontWeight = FontWeight.Medium, color = colors.accent)
            ProgressLine(progress = state.progress, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
        }
        is NoteRowState.Attention -> Text(
            text = state.text,
            style = OffhandTheme.type.bodySmall,
            fontWeight = FontWeight.Medium,
            color = colors.ink2,
        )
    }
}

@Composable
fun WaveGlyph(
    seed: Long,
    modifier: Modifier = Modifier,
    levels: List<Float> = emptyList(),
    color: Color = OffhandTheme.colors.wave,
) {
    val bars = remember(seed, levels) { levels.toBarHeights().ifEmpty { waveBars(seed, GLYPH_BARS) } }
    Canvas(modifier = modifier.size(44.dp, 24.dp)) {
        val step = size.width / bars.size
        val stroke = 2.dp.toPx()
        bars.forEachIndexed { index, height ->
            val x = step * index + step / 2
            val half = size.height * height / 2
            drawLine(color, Offset(x, size.height / 2 - half), Offset(x, size.height / 2 + half), stroke, StrokeCap.Round)
        }
    }
}

fun List<Float>.toBarHeights(): List<Float> = map { level -> MIN_BAR + level.coerceIn(0f, 1f) * (1f - MIN_BAR) }

fun waveBars(seed: Long, count: Int): List<Float> {
    val random = Random(seed)
    return List(count) { MIN_BAR + random.nextFloat() * (1f - MIN_BAR) }
}

private const val GLYPH_BARS = 9
private const val MIN_BAR = 0.18f
