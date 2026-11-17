package com.dmytrosamoilov.offhand.feature.recording.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmytrosamoilov.offhand.core.designsystem.haptics.haptics
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandDarkContent
import com.dmytrosamoilov.offhand.core.designsystem.theme.OffhandTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun RecordingSheetHost(
    isVisible: Boolean,
    folderId: Long?,
    onVisibilityChange: (Boolean) -> Unit,
    onNoteSaved: (noteId: Long) -> Unit,
    viewModel: RecordingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onNotificationPromptAnswered(granted) }

    LaunchedEffect(state.phase, isVisible) {
        if (state.phase == RecordingPhaseUi.RECORDING && !isVisible) {
            onVisibilityChange(true)
        }
    }
    LaunchedEffect(state.isNotificationPromptRequested) {
        if (!state.isNotificationPromptRequested) return@LaunchedEffect
        if (needsNotificationPermission(context)) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.onNotificationPromptSkipped()
        }
    }

    if (isVisible) {
        OffhandDarkContent {
            RecordingBottomSheet(
                state = state,
                viewModel = viewModel,
                folderId = folderId,
                onDismiss = {
                    viewModel.onSheetClosed()
                    onVisibilityChange(false)
                },
                onNoteSaved = { noteId ->
                    viewModel.onSheetClosed()
                    onVisibilityChange(false)
                    onNoteSaved(noteId)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordingBottomSheet(
    state: RecordingUiState,
    viewModel: RecordingViewModel,
    folderId: Long?,
    onDismiss: () -> Unit,
    onNoteSaved: (noteId: Long) -> Unit,
) {
    val isCapturing by rememberUpdatedState(state.phase == RecordingPhaseUi.RECORDING)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target -> target != SheetValue.Hidden || !isCapturing },
    )
    var wasSessionActive by rememberSaveable { mutableStateOf(false) }
    val savedNoteId = state.savedNoteId?.takeIf { wasSessionActive }
    val isSessionFinished = wasSessionActive && savedNoteId == null &&
        state.phase == RecordingPhaseUi.IDLE

    LaunchedEffect(Unit) { viewModel.onSheetOpened() }
    LaunchedEffect(savedNoteId) {
        if (savedNoteId != null) {
            onNoteSaved(savedNoteId)
        }
    }
    LaunchedEffect(state.phase) {
        if (state.phase.isSessionActive()) {
            wasSessionActive = true
        }
    }
    LaunchedEffect(isSessionFinished) {
        if (isSessionFinished) {
            onDismiss()
        }
    }
    LaunchedEffect(state.phase) {
        sheetState.expand()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RectangleShape,
        containerColor = OffhandTheme.colors.ground,
        contentColor = OffhandTheme.colors.ink,
        scrimColor = OffhandTheme.colors.scrim,
        tonalElevation = 0.dp,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0.dp) },
    ) {
        LightSystemBarIcons()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
        ) {
            if (savedNoteId == null && !isSessionFinished) {
                SheetPhaseContent(state = state, viewModel = viewModel, folderId = folderId, onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun ColumnScope.SheetPhaseContent(
    state: RecordingUiState,
    viewModel: RecordingViewModel,
    folderId: Long?,
    onDismiss: () -> Unit,
) {
    val haptics = haptics()
    when (state.phase) {
        RecordingPhaseUi.IDLE -> IdleContent(state = state, onRecordClick = { viewModel.onStartRecording(folderId) })
        RecordingPhaseUi.RECORDING -> RecordingContent(
            state = state,
            actions = RecordingActions(
                onPause = {
                    haptics.tick()
                    viewModel.onPauseRecording()
                },
                onResume = {
                    haptics.tick()
                    viewModel.onResumeRecording()
                },
                onSave = {
                    haptics.confirm()
                    viewModel.onStopRecording()
                },
                onDiscard = {
                    haptics.reject()
                    viewModel.onDiscardRecording()
                    onDismiss()
                },
            ),
        )
        RecordingPhaseUi.FINISHING_TRANSCRIPTION -> FinishingContent(state = state)
        RecordingPhaseUi.FAILED -> FailedContent(state = state, onRetry = { viewModel.onStartRecording(folderId) })
    }
}

@Composable
private fun LightSystemBarIcons() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val view = LocalView.current
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
}

private fun RecordingPhaseUi.isSessionActive(): Boolean =
    this == RecordingPhaseUi.RECORDING || this == RecordingPhaseUi.FINISHING_TRANSCRIPTION

internal fun hasMicPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
