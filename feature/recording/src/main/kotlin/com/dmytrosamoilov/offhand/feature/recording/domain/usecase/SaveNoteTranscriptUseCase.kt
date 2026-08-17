package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import javax.inject.Inject

class SaveNoteTranscriptUseCase @Inject constructor(
    private val notesRepository: NotesRepository,
) {
    suspend operator fun invoke(
        noteId: Long,
        transcript: String,
        transcriptionTimeMs: Long,
    ) {
        val note = notesRepository.getNote(noteId) ?: return
        notesRepository.updateNote(
            note.copy(
                transcript = transcript,
                transcriptionTimeMs = transcriptionTimeMs,
            ),
        )
    }
}
