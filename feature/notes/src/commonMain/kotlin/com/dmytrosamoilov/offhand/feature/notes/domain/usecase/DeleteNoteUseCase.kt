package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository

class DeleteNoteUseCase(
    private val repository: NotesRepository,
) {
    suspend operator fun invoke(id: Long) = repository.deleteNote(id)
}
