package com.dmytrosamoilov.offhand.feature.recording.presentation

data class RecordingUiState(
    val phase: RecordingPhaseUi = RecordingPhaseUi.IDLE,
    val isPaused: Boolean = false,
    val elapsedTime: String = "00:00",
    val elapsedMs: Long = 0,
    val waveform: List<Float> = emptyList(),
    val isSilent: Boolean = true,
    val chunks: List<ChunkUi> = emptyList(),
    val failureMessage: String? = null,
    val savedNoteId: Long? = null,
    val isDeveloperMode: Boolean = false,
    val externalMicName: String? = null,
    val isNotificationPromptRequested: Boolean = false,
    val isLiveTranscriptShown: Boolean = false,
    val liveTranscript: String = "",
    val folderName: String? = null,
)

enum class RecordingPhaseUi {
    IDLE,
    RECORDING,
    FINISHING_TRANSCRIPTION,
    FAILED,
}

data class ChunkUi(
    val id: Int,
    val state: ChunkStateUi,
)

enum class ChunkStateUi {
    QUEUED,
    TRANSCRIBING,
    DONE,
    FAILED,
}
