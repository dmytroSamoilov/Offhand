package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository

class MarkNoteRecordedUseCase(
    private val notesRepository: NotesRepository,
) {
    suspend operator fun invoke(
        noteId: Long,
        durationMs: Long,
        audioFileName: String?,
    ): Note? {
        val note = notesRepository.getNote(noteId) ?: return null
        val recorded = note.copy(
            status = NoteStatus.PROCESSING,
            durationMs = durationMs,
            audioFileName = audioFileName,
        )
        notesRepository.updateNote(recorded)
        return recorded
    }
}
