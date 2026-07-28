package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import javax.inject.Inject

class MarkNoteProcessingUseCase @Inject constructor(
    private val notesRepository: NotesRepository,
) {
    suspend operator fun invoke(noteId: Long): Note? {
        val note = notesRepository.getNote(noteId) ?: return null
        val processing = note.copy(status = NoteStatus.PROCESSING)
        notesRepository.updateNote(processing)
        return processing
    }
}
