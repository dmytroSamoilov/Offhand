package com.dmytrosamoilov.offhand.feature.recording.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.component.CookieShape
import com.dmytrosamoilov.offhand.core.designsystem.component.MorphingLoadingIndicator
import com.dmytrosamoilov.offhand.feature.recording.R

@Composable
fun RecordingSheetHost(
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    viewModel: RecordingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.phase, isVisible) {
        if (state.phase == RecordingPhaseUi.RECORDING && !isVisible) {
            onVisibilityChange(true)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.reviewRequests.collect {
            val activity = context.findActivity() ?: return@collect
            if (requestInAppReview(activity)) {
                viewModel.onReviewFlowCompleted()
            }
        }
    }

    if (isVisible) {
        RecordingBottomSheet(
            state = state,
            viewModel = viewModel,
            onDismiss = {
                viewModel.onSheetClosed()
                onVisibilityChange(false)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordingBottomSheet(
    state: RecordingUiState,
    viewModel: RecordingViewModel,
    onDismiss: () -> Unit,
) {
    val isCapturing by rememberUpdatedState(state.phase == RecordingPhaseUi.RECORDING)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target -> target != SheetValue.Hidden || !isCapturing },
    )
    var isNoteSaved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.onSheetOpened() }
    LaunchedEffect(state.savedNoteId) {
        if (state.savedNoteId != null) {
            isNoteSaved = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                isNoteSaved -> SavedContent(onDone = onDismiss)
                state.phase == RecordingPhaseUi.IDLE -> IdleContent(
                    onRecordClick = viewModel::onStartRecording,
                )
                state.phase == RecordingPhaseUi.RECORDING -> RecordingContent(
                    state = state,
                    onPauseClick = viewModel::onPauseRecording,
                    onResumeClick = viewModel::onResumeRecording,
                    onStopClick = viewModel::onStopRecording,
                    onDiscardConfirmed = {
                        viewModel.onDiscardRecording()
                        onDismiss()
                    },
                )
                state.phase == RecordingPhaseUi.FINISHING_TRANSCRIPTION -> ProcessingContent(
                    stageText = stringResource(R.string.recording_stage_finishing),
                    chunks = if (state.isDeveloperMode) state.chunks else emptyList(),
                )
                else -> FailedContent(
                    message = state.failureMessage,
                    onRetry = viewModel::onStartRecording,
                )
            }
        }
    }
}

@Composable
private fun SavedContent(onDone: () -> Unit) {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.size(64.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.recording_saved_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.recording_saved_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onDone) {
        Text(text = stringResource(R.string.recording_saved_done_button))
    }
}

@Composable
private fun IdleContent(onRecordClick: () -> Unit) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) {
            onRecordClick()
        }
    }

    Text(
        text = stringResource(R.string.recording_idle_hint),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(32.dp))
    FilledIconButton(
        onClick = {
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasMic) {
                onRecordClick()
            } else {
                permissionLauncher.launch(requiredPermissions())
            }
        },
        modifier = Modifier.size(96.dp),
        shape = CookieShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = stringResource(R.string.recording_start_description),
            modifier = Modifier.size(40.dp),
        )
    }
}

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.RECORD_AUDIO)
    }

@Composable
private fun RecordingContent(
    state: RecordingUiState,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    onDiscardConfirmed: () -> Unit,
) {
    var isDiscardDialogVisible by remember { mutableStateOf(false) }

    WaveformVisualizer(
        levels = state.waveform,
        isPaused = state.isPaused,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = state.elapsedTime,
        style = MaterialTheme.typography.displayMedium,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(
            if (state.isPaused) R.string.recording_paused_label else R.string.recording_active_label,
        ),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state.isDeveloperMode && state.chunks.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        ChunkPills(chunks = state.chunks)
    }
    if (state.externalMicName != null) {
        Spacer(modifier = Modifier.height(12.dp))
        ExternalMicBadge(name = state.externalMicName)
    }
    Spacer(modifier = Modifier.height(28.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PauseResumeButton(
            isPaused = state.isPaused,
            onPauseClick = onPauseClick,
            onResumeClick = onResumeClick,
        )
        Button(
            onClick = onStopClick,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.recording_save_note_button),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = { isDiscardDialogVisible = true }) {
        Text(
            text = stringResource(R.string.recording_discard_button),
            color = MaterialTheme.colorScheme.error,
        )
    }

    if (isDiscardDialogVisible) {
        DiscardConfirmationDialog(
            onConfirm = {
                isDiscardDialogVisible = false
                onDiscardConfirmed()
            },
            onDismiss = { isDiscardDialogVisible = false },
        )
    }
}

@Composable
private fun PauseResumeButton(
    isPaused: Boolean,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = if (isPaused) onResumeClick else onPauseClick,
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
    ) {
        Icon(
            imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            contentDescription = stringResource(
                if (isPaused) {
                    R.string.recording_resume_description
                } else {
                    R.string.recording_pause_description
                },
            ),
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun DiscardConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.recording_discard_dialog_title)) },
        text = { Text(text = stringResource(R.string.recording_discard_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.recording_discard_dialog_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.recording_discard_dialog_cancel))
            }
        },
    )
}

@Composable
private fun WaveformVisualizer(
    levels: List<Float>,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    val barColor = if (isPaused) {
        MaterialTheme.colorScheme.outline
    } else {
        MaterialTheme.colorScheme.primary
    }
    Canvas(modifier = modifier.height(88.dp)) {
        val barWidth = 3.dp.toPx()
        val step = barWidth + 2.dp.toPx()
        val maxBars = (size.width / step).toInt()
        if (maxBars <= 0) return@Canvas
        val visible = levels.takeLast(maxBars)
        drawCenterLine(barColor)
        val startX = (size.width - visible.size * step) / 2f
        visible.forEachIndexed { index, level ->
            val barHeight = (level * size.height).coerceAtLeast(barWidth)
            val x = startX + index * step
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

private fun DrawScope.drawCenterLine(color: Color) {
    drawLine(
        color = color.copy(alpha = 0.3f),
        start = Offset(0f, size.height / 2f),
        end = Offset(size.width, size.height / 2f),
        strokeWidth = 1.dp.toPx(),
    )
}

@Composable
private fun ExternalMicBadge(name: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.HeadsetMic,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = name.ifBlank { stringResource(R.string.recording_external_mic_generic) },
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ChunkPills(chunks: List<ChunkUi>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(chunks, key = { it.id }) { chunk ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(chunk.state.indicatorColor()),
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.recording_chunk_label, chunk.id),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChunkStateUi.indicatorColor() = when (this) {
    ChunkStateUi.QUEUED -> MaterialTheme.colorScheme.outline
    ChunkStateUi.TRANSCRIBING -> MaterialTheme.colorScheme.tertiary
    ChunkStateUi.DONE -> MaterialTheme.colorScheme.primary
    ChunkStateUi.FAILED -> MaterialTheme.colorScheme.error
}

@Composable
private fun ProcessingContent(stageText: String, chunks: List<ChunkUi>) {
    MorphingLoadingIndicator()
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stageText,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    if (chunks.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        ChunkPills(chunks = chunks)
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.recording_processing_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun FailedContent(
    message: String?,
    onRetry: () -> Unit,
) {
    Text(
        text = stringResource(R.string.recording_failed_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    if (message != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onRetry) {
        Text(text = stringResource(R.string.recording_try_again_button))
    }
}
