package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.TranscriptionCheckpoint
import com.dmytrosamoilov.offhand.core.data.domain.TranscriptionCheckpointRepository

class GetTranscriptionCheckpointUseCase(
    private val repository: TranscriptionCheckpointRepository,
) {
    suspend operator fun invoke(noteId: Long): TranscriptionCheckpoint? = repository.getCheckpoint(noteId)
}
