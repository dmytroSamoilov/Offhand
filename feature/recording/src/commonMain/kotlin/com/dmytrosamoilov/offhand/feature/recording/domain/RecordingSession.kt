package com.dmytrosamoilov.offhand.feature.recording.domain

data class RecordingSession(
    val phase: SessionPhase = SessionPhase.IDLE,
    val isPaused: Boolean = false,
    val chunks: List<SessionChunk> = emptyList(),
    val transcriptionTimeMs: Long = 0,
    val noteId: Long? = null,
    val errorMessage: String? = null,
)

enum class SessionPhase {
    IDLE,
    RECORDING,
    DRAINING,
    FAILED,
}

data class SessionChunk(
    val id: Int,
    val durationMs: Long,
    val state: ChunkState,
)

enum class ChunkState {
    QUEUED,
    TRANSCRIBING,
    DONE,
    FAILED,
}

sealed interface NoteProcessingEvent {
    val noteId: Long

    data class Completed(override val noteId: Long) : NoteProcessingEvent
    data class Failed(override val noteId: Long) : NoteProcessingEvent
}
