package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository

class DiscardNoteUseCase(
    private val notesRepository: NotesRepository,
) {
    suspend operator fun invoke(noteId: Long) {
        notesRepository.deleteNote(noteId)
    }
}
