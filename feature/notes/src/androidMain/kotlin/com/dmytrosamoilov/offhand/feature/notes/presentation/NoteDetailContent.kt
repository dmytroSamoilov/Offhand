package com.dmytrosamoilov.offhand.feature.notes.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.component.MarkdownText
import com.dmytrosamoilov.offhand.core.designsystem.component.MessageCard
import com.dmytrosamoilov.offhand.core.designsystem.component.PrimaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.ProgressLine
import com.dmytrosamoilov.offhand.core.designsystem.component.SecondaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.Skeleton
import com.dmytrosamoilov.offhand.core.designsystem.component.TimeMark
import com.dmytrosamoilov.offhand.core.designsystem.haptics.haptics
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.notes.R
import kotlinx.coroutines.delay

@Composable
internal fun NoteDetailBody(
    state: NotesUiState,
    viewModel: NotesViewModel,
    note: NoteDetailUi,
    tabs: NoteDetailTabsUi,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 24.dp)) {
        if (state.isDeveloperMode) note.metrics?.let { DeveloperMetrics(it) }
        ProcessingHaptics(noteId = note.id, status = note.status)
        when (tabs.selected) {
            null -> PlainNotice(text = stringResource(R.string.notes_audio_only))
            NoteDetailTabUi.SUMMARY -> SummaryTab(state = state, viewModel = viewModel, note = note)
            NoteDetailTabUi.TRANSCRIPT -> TranscriptTab(state = state, viewModel = viewModel, note = note)
        }
    }
}

@Composable
private fun DeveloperMetrics(metrics: NoteMetricsUi) {
    Text(
        text = stringResource(R.string.notes_metrics, metrics.transcriptionTime, metrics.structuringTime, metrics.hardwareBackend),
        style = OffhandTheme.type.caption,
        color = OffhandTheme.colors.ink3,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
    )
}

@Composable
private fun SummaryTab(state: NotesUiState, viewModel: NotesViewModel, note: NoteDetailUi) {
    val contentModifier = Modifier.padding(horizontal = 24.dp)
    when (note.status) {
        NoteStatusUi.PROCESSING -> WorkingState(
            text = stringResource(note.summaryWritingText(state.isWaitingForAi)),
            percent = state.noteProgress[note.id],
            isWorking = !state.isWaitingForAi,
            hint = stringResource(R.string.notes_processing_summary_hint).takeIf { note.transcript.isNotBlank() && !state.isWaitingForAi },
            modifier = contentModifier,
        )
        NoteStatusUi.FAILED -> FailedCard(note = note, onRetry = viewModel::onRetryTranscriptionRequested, modifier = contentModifier)
        NoteStatusUi.INTERRUPTED -> PausedCard(onResume = viewModel::onResumeProcessingRequested, modifier = contentModifier)
        NoteStatusUi.READY -> MarkdownText(markdown = note.body, modifier = contentModifier)
    }
    if (state.styleChips.isNotEmpty()) {
        StyleChipsRow(
            chips = state.styleChips,
            onStyleSelected = viewModel::onStyleSelected,
            modifier = Modifier.padding(top = 28.dp),
        )
    }
    state.smartSuggestions?.let { suggestions ->
        SmartSuggestionsCard(
            suggestions = suggestions,
            actions = SuggestionActions(
                onFind = viewModel::onSuggestionsRequested,
                onAdd = viewModel::onSuggestionAddRequested,
                onDismiss = viewModel::onSuggestionDismissed,
            ),
            modifier = contentModifier.padding(top = 28.dp),
        )
    }
}

@Composable
private fun TranscriptTab(state: NotesUiState, viewModel: NotesViewModel, note: NoteDetailUi) {
    val contentModifier = Modifier.padding(horizontal = 24.dp)
    val percent = state.noteProgress[note.id]
    when {
        note.transcript.isNotBlank() -> {
            TranscriptText(
                note = note,
                onTimeSelected = (viewModel::onTranscriptTimeSelected).takeIf { note.hasAudio && state.playback.isAvailable },
            )
            if (state.isTranscribing) {
                WorkingState(
                    text = stringResource(R.string.notes_transcript_more_coming),
                    percent = percent,
                    isWorking = true,
                    modifier = contentModifier.padding(top = 24.dp),
                )
            }
        }
        note.status == NoteStatusUi.PROCESSING -> WorkingState(
            text = stringResource(transcriptWritingText(state.isWaitingForSpeech)),
            percent = percent,
            isWorking = !state.isWaitingForSpeech,
            modifier = contentModifier,
        )
        note.status == NoteStatusUi.FAILED -> FailedCard(note = note, onRetry = viewModel::onRetryTranscriptionRequested, modifier = contentModifier)
        note.status == NoteStatusUi.INTERRUPTED -> PausedCard(onResume = viewModel::onResumeProcessingRequested, modifier = contentModifier)
        else -> PlainNotice(text = stringResource(R.string.notes_transcript_empty))
    }
}

@Composable
private fun TranscriptText(note: NoteDetailUi, onTimeSelected: ((Long) -> Unit)?) {
    if (note.transcriptParts.isEmpty()) {
        MarkdownText(markdown = note.transcript, modifier = Modifier.padding(horizontal = 24.dp))
        return
    }
    Column(modifier = Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        note.transcriptParts.forEach { part -> TranscriptParagraph(part = part, onTimeSelected = onTimeSelected) }
    }
}

@Composable
private fun TranscriptParagraph(part: TranscriptPartUi, onTimeSelected: ((Long) -> Unit)?) {
    val description = stringResource(R.string.notes_transcript_time_description, part.time)
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(
                onTimeSelected?.let { onSelected -> Modifier.clickable(onClickLabel = description) { onSelected(part.startMs) } }
                    ?: Modifier,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        TimeMark(text = part.time)
        MarkdownText(markdown = part.text)
    }
}

@Composable
private fun WorkingState(
    text: String,
    percent: Int?,
    isWorking: Boolean,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    val colors = OffhandTheme.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = text,
                style = if (isWorking) OffhandTheme.type.buttonSmall else OffhandTheme.type.dialogBody,
                color = if (isWorking) colors.accent else colors.ink2,
                modifier = Modifier.weight(1f),
            )
            percent?.let { Text(text = stringResource(R.string.notes_processing_percent, it), style = OffhandTheme.type.number, color = colors.ink2) }
        }
        if (isWorking) {
            ProgressLine(progress = percent?.let { it / PERCENT })
            Skeleton(modifier = Modifier.padding(top = 8.dp))
        }
        hint?.let { Text(text = it, style = OffhandTheme.type.caption, color = colors.ink2, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun PausedCard(onResume: () -> Unit, modifier: Modifier = Modifier) {
    MessageCard(
        title = stringResource(R.string.notes_interrupted_title),
        body = stringResource(R.string.notes_interrupted_description),
        modifier = modifier,
    ) {
        PrimaryButton(
            text = stringResource(R.string.notes_continue_button),
            onClick = onResume,
            height = CARD_BUTTON_HEIGHT,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
    }
}

@Composable
private fun FailedCard(note: NoteDetailUi, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val hasTranscript = note.transcript.isNotBlank()
    MessageCard(
        title = stringResource(if (hasTranscript) R.string.notes_failed_summary_title else R.string.notes_failed_title),
        body = stringResource(if (hasTranscript) R.string.notes_failed_summary_description else R.string.notes_failed_description),
        modifier = modifier,
    ) {
        if (note.hasAudio) {
            SecondaryButton(
                text = stringResource(R.string.notes_try_again_button),
                onClick = onRetry,
                height = CARD_BUTTON_HEIGHT,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun PlainNotice(text: String) {
    Text(
        text = text,
        style = OffhandTheme.type.body,
        fontWeight = FontWeight.Normal,
        color = OffhandTheme.colors.ink2,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
}

@Composable
private fun ProcessingHaptics(noteId: Long, status: NoteStatusUi) {
    val haptics = haptics()
    var previousStatus by remember(noteId) { mutableStateOf(status) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(noteId, status, lifecycleOwner) {
        if (previousStatus == NoteStatusUi.PROCESSING && status == NoteStatusUi.READY) haptics.confirm()
        previousStatus = status
        if (status != NoteStatusUi.PROCESSING) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(PROCESSING_TICK_MS)
                haptics.tick()
            }
        }
    }
}

private fun NoteDetailUi.summaryWritingText(isWaitingForAi: Boolean): Int = when {
    isWaitingForAi && transcript.isBlank() -> R.string.notes_processing_waiting_transcribing
    isWaitingForAi -> R.string.notes_processing_waiting_structuring
    else -> R.string.notes_processing_structuring
}

private fun transcriptWritingText(isWaitingForSpeech: Boolean): Int =
    if (isWaitingForSpeech) R.string.notes_processing_waiting_transcribing else R.string.notes_processing_transcribing

private const val PROCESSING_TICK_MS = 2_500L
private const val PERCENT = 100f
private val CARD_BUTTON_HEIGHT = 48.dp
