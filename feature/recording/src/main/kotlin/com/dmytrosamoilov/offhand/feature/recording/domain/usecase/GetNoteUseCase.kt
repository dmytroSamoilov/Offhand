package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository

class GetNoteUseCase(
    private val notesRepository: NotesRepository,
) {
    suspend operator fun invoke(noteId: Long): Note? = notesRepository.getNote(noteId)
}
