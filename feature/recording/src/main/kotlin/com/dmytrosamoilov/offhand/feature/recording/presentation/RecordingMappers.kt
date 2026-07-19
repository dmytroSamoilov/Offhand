package com.dmytrosamoilov.offhand.feature.recording.presentation

import com.dmytrosamoilov.offhand.core.audio.VadSnapshot
import com.dmytrosamoilov.offhand.feature.recording.domain.ChunkState
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSession
import com.dmytrosamoilov.offhand.feature.recording.domain.SessionPhase
import java.util.Locale

private const val MIN_LEVEL_DB = -60f

internal fun normalizedAudioLevel(rmsDb: Float): Float =
    ((rmsDb - MIN_LEVEL_DB) / -MIN_LEVEL_DB).coerceIn(0f, 1f)

internal fun toRecordingUiState(
    session: RecordingSession,
    vad: VadSnapshot,
    waveform: List<Float>,
    isDeveloperMode: Boolean,
    externalMicName: String?,
): RecordingUiState =
    RecordingUiState(
        phase = session.phase.toUi(),
        isPaused = session.isPaused,
        elapsedTime = formatElapsed(vad.totalElapsedMs),
        waveform = waveform,
        isSilent = vad.isSilent,
        chunks = session.chunks.map { ChunkUi(id = it.id, state = it.state.toUi()) },
        failureMessage = session.errorMessage,
        savedNoteId = session.noteId,
        isDeveloperMode = isDeveloperMode,
        externalMicName = externalMicName,
    )

private fun SessionPhase.toUi(): RecordingPhaseUi = when (this) {
    SessionPhase.IDLE -> RecordingPhaseUi.IDLE
    SessionPhase.RECORDING -> RecordingPhaseUi.RECORDING
    SessionPhase.DRAINING -> RecordingPhaseUi.FINISHING_TRANSCRIPTION
    SessionPhase.FAILED -> RecordingPhaseUi.FAILED
}

private fun ChunkState.toUi(): ChunkStateUi = when (this) {
    ChunkState.QUEUED -> ChunkStateUi.QUEUED
    ChunkState.TRANSCRIBING -> ChunkStateUi.TRANSCRIBING
    ChunkState.DONE -> ChunkStateUi.DONE
    ChunkState.FAILED -> ChunkStateUi.FAILED
}

internal fun formatElapsed(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
