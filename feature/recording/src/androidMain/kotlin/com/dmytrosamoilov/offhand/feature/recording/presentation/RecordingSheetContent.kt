package com.dmytrosamoilov.offhand.feature.recording.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandDialog
import com.dmytrosamoilov.offhand.core.designsystem.component.OffhandIcons
import com.dmytrosamoilov.offhand.core.designsystem.component.PlainTextButton
import com.dmytrosamoilov.offhand.core.designsystem.component.PrimaryButton
import com.dmytrosamoilov.offhand.core.designsystem.component.RoundIconButton
import com.dmytrosamoilov.offhand.core.designsystem.component.SectionLabel
import com.dmytrosamoilov.offhand.core.designsystem.component.TextButtonTone
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import com.dmytrosamoilov.offhand.feature.recording.R

internal data class RecordingActions(
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onSave: () -> Unit,
    val onDiscard: () -> Unit,
)

@Composable
internal fun ColumnScope.IdleContent(state: RecordingUiState, onRecordClick: () -> Unit) {
    val context = LocalContext.current
    var isPermissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) onRecordClick() else isPermissionDenied = true
    }
    LaunchedEffect(Unit) {
        if (hasMicPermission(context)) onRecordClick() else permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
    }
    TimerBlock(elapsedTime = state.elapsedTime, modifier = Modifier.padding(top = 108.dp))
    Spacer(modifier = Modifier.weight(1f))
    if (isPermissionDenied) {
        MicPermissionFallback(onRetry = { permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO)) })
    }
}

@Composable
private fun MicPermissionFallback(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = stringResource(R.string.recording_idle_hint),
            style = OffhandTheme.type.body,
            color = OffhandTheme.colors.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = stringResource(R.string.recording_start_description),
            onClick = onRetry,
            icon = OffhandIcons.Mic,
            height = SAVE_HEIGHT,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun ColumnScope.RecordingContent(state: RecordingUiState, actions: RecordingActions) {
    var isDiscardDialogVisible by remember { mutableStateOf(false) }
    RecordingHeader(isPaused = state.isPaused, folderName = state.folderName)
    TimerBlock(elapsedTime = state.elapsedTime, modifier = Modifier.padding(top = 64.dp))
    state.externalMicName?.let { ExternalMicLine(name = it) }
    RecordingWaveform(
        levels = state.waveform,
        isPaused = state.isPaused,
        modifier = Modifier.fillMaxWidth().padding(top = 44.dp),
    )
    if (state.isLiveTranscriptShown) LiveTranscript(text = state.liveTranscript)
    if (state.isDeveloperMode) ChunkStates(chunks = state.chunks)
    Spacer(modifier = Modifier.weight(1f))
    RecordingFooter(
        isPaused = state.isPaused,
        actions = actions,
        onDiscardRequested = { isDiscardDialogVisible = true },
    )
    if (isDiscardDialogVisible) {
        DiscardDialog(
            elapsedMs = state.elapsedMs,
            onDiscard = {
                isDiscardDialogVisible = false
                actions.onDiscard()
            },
            onKeep = { isDiscardDialogVisible = false },
        )
    }
}

@Composable
private fun RecordingHeader(isPaused: Boolean, folderName: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecordingStatus(isPaused = isPaused, modifier = Modifier.weight(1f))
        folderName?.let { FolderLabel(name = it) }
    }
}

@Composable
private fun RecordingStatus(isPaused: Boolean, modifier: Modifier = Modifier) {
    val colors = OffhandTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatusDot(color = if (isPaused) colors.ink2 else colors.record, size = 8.dp)
        Text(
            text = stringResource(if (isPaused) R.string.recording_paused_label else R.string.recording_active_label).uppercase(),
            style = OffhandTheme.type.label.copy(letterSpacing = STATUS_TRACKING),
            color = colors.ink,
        )
    }
}

@Composable
private fun FolderLabel(name: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconGlyph(icon = OffhandIcons.Folder, size = 16.dp)
        Text(
            text = name,
            style = OffhandTheme.type.caption,
            color = OffhandTheme.colors.ink2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TimerBlock(elapsedTime: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = elapsedTime, style = OffhandTheme.type.timer, color = OffhandTheme.colors.ink, maxLines = 1)
        IconLine(icon = OffhandIcons.Lock, text = stringResource(R.string.recording_notification_recording_text))
    }
}

@Composable
private fun ExternalMicLine(name: String) {
    IconLine(
        icon = OffhandIcons.Mic,
        text = name.ifBlank { stringResource(R.string.recording_external_mic_generic) },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

@Composable
private fun LiveTranscript(text: String) {
    Column(modifier = Modifier.padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = stringResource(R.string.recording_live_label))
        Text(
            text = text.ifBlank { stringResource(R.string.recording_live_placeholder) },
            style = OffhandTheme.type.body,
            color = if (text.isBlank()) OffhandTheme.colors.ink3 else OffhandTheme.colors.ink,
            minLines = LIVE_TRANSCRIPT_LINES,
            maxLines = LIVE_TRANSCRIPT_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RecordingFooter(isPaused: Boolean, actions: RecordingActions, onDiscardRequested: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            PauseResumeControl(isPaused = isPaused, onPause = actions.onPause, onResume = actions.onResume)
            PrimaryButton(
                text = stringResource(R.string.recording_save_note_button),
                onClick = actions.onSave,
                icon = OffhandIcons.Check,
                height = SAVE_HEIGHT,
                modifier = Modifier.weight(1f),
            )
        }
        PlainTextButton(
            text = stringResource(R.string.recording_discard_button),
            onClick = onDiscardRequested,
            tone = TextButtonTone.Danger,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun PauseResumeControl(isPaused: Boolean, onPause: () -> Unit, onResume: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RoundIconButton(
            icon = if (isPaused) OffhandIcons.Play else OffhandIcons.PauseBars,
            contentDescription = stringResource(
                if (isPaused) R.string.recording_resume_description else R.string.recording_pause_description,
            ),
            onClick = if (isPaused) onResume else onPause,
            size = SAVE_HEIGHT,
        )
        Text(
            text = stringResource(if (isPaused) R.string.recording_resume_label else R.string.recording_pause_label),
            style = OffhandTheme.type.caption,
            color = OffhandTheme.colors.ink2,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun DiscardDialog(elapsedMs: Long, onDiscard: () -> Unit, onKeep: () -> Unit) {
    OffhandDialog(
        title = stringResource(R.string.recording_discard_dialog_title),
        body = stringResource(R.string.recording_discard_dialog_body, durationPhrase(elapsedMs)),
        onDismissRequest = onKeep,
        confirmLabel = stringResource(R.string.recording_discard_keep),
        onConfirm = onKeep,
        destructiveLabel = stringResource(R.string.recording_discard_dialog_confirm),
        onDestructive = onDiscard,
    )
}

@Composable
private fun durationPhrase(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / MS_PER_SECOND).toInt()
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return listOfNotNull(
        hours.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.recording_duration_hours, it, it) },
        minutes.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.recording_duration_minutes, it, it) },
        seconds.takeIf { it > 0 || totalSeconds == 0 }?.let { pluralStringResource(R.plurals.recording_duration_seconds, it, it) },
    ).joinToString(separator = " ")
}

@Composable
internal fun ColumnScope.FinishingContent(state: RecordingUiState) {
    TimerBlock(elapsedTime = state.elapsedTime, modifier = Modifier.padding(top = 108.dp))
    Spacer(modifier = Modifier.weight(1f))
    if (state.isDeveloperMode) ChunkStates(chunks = state.chunks)
    StatusMessage(
        title = stringResource(R.string.recording_stage_finishing),
        body = stringResource(R.string.recording_processing_hint),
    )
}

@Composable
internal fun ColumnScope.FailedContent(state: RecordingUiState, onRetry: () -> Unit) {
    Spacer(modifier = Modifier.weight(1f))
    StatusMessage(title = stringResource(R.string.recording_failed_title), body = state.failureMessage)
    PrimaryButton(
        text = stringResource(R.string.recording_try_again_button),
        onClick = onRetry,
        height = SAVE_HEIGHT,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    )
}

@Composable
private fun StatusMessage(title: String, body: String?) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = title, style = OffhandTheme.type.heading, color = OffhandTheme.colors.ink)
        body?.let { Text(text = it, style = OffhandTheme.type.dialogBody, color = OffhandTheme.colors.ink2) }
    }
}

private val SAVE_HEIGHT = 64.dp
private val STATUS_TRACKING = 0.12.em
private const val LIVE_TRANSCRIPT_LINES = 3
private const val MS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3600
