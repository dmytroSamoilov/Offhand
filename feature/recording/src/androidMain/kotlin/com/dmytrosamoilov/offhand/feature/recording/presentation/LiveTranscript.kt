package com.dmytrosamoilov.offhand.feature.recording.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dmytrosamoilov.offhand.core.designsystem.component.SectionLabel
import com.dmytrosamoilov.offhand.core.designsystem.component.isReducedMotion
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.recording.R
import kotlinx.coroutines.delay

@Composable
internal fun LiveTranscript(text: String, isWorking: Boolean) {
    val activeDot = if (isWorking) workingDotIndex() else NO_DOT
    Column(modifier = Modifier.padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = stringResource(R.string.recording_live_label))
        Text(
            text = liveText(text = text, isWorking = isWorking, activeDot = activeDot),
            style = OffhandTheme.type.body,
            color = OffhandTheme.colors.ink,
            minLines = LIVE_TRANSCRIPT_LINES,
            maxLines = LIVE_TRANSCRIPT_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = text },
        )
    }
}

@Composable
private fun liveText(text: String, isWorking: Boolean, activeDot: Int): AnnotatedString {
    val active = OffhandTheme.colors.ink
    val resting = OffhandTheme.colors.ink3
    return buildAnnotatedString {
        append(text)
        if (!isWorking) return@buildAnnotatedString
        if (text.isNotBlank()) append(" ")
        repeat(DOT_COUNT) { index -> appendDot(color = if (index == activeDot) active else resting) }
    }
}

private fun AnnotatedString.Builder.appendDot(color: Color) {
    withStyle(SpanStyle(color = color)) { append(DOT) }
}

@Composable
private fun workingDotIndex(): Int {
    if (isReducedMotion()) return NO_DOT
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(DOT_STEP_MS)
            index = (index + 1) % DOT_COUNT
        }
    }
    return index
}

private const val LIVE_TRANSCRIPT_LINES = 3
private const val DOT_COUNT = 3
private const val NO_DOT = -1
private const val DOT_STEP_MS = 350L
private const val DOT = "\u2022"
