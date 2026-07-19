package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import javax.inject.Inject

class CreateProcessingNoteUseCase @Inject constructor(
    private val notesRepository: NotesRepository,
) {
    suspend operator fun invoke(audioFileName: String?, durationMs: Long?): Long =
        notesRepository.createNote(
            Note(
                id = 0,
                title = "",
                body = "",
                transcript = "",
                createdAtEpochMs = System.currentTimeMillis(),
                transcriptionTimeMs = null,
                structuringTimeMs = null,
                hardwareBackend = null,
                audioFileName = audioFileName,
                durationMs = durationMs,
                status = NoteStatus.PROCESSING,
            ),
        )
}
