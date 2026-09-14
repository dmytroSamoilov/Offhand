package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.TranscriptionCheckpointRepository

class ClearTranscriptionCheckpointUseCase(
    private val repository: TranscriptionCheckpointRepository,
) {
    suspend operator fun invoke(noteId: Long) = repository.clearCheckpoint(noteId)
}
