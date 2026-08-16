package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository

class UpdateNoteUseCase(
    private val repository: NotesRepository,
) {
    suspend operator fun invoke(note: Note) = repository.updateNote(note)
}
