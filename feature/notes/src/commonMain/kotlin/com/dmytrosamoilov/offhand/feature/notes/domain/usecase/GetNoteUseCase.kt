package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository

class GetNoteUseCase(
    private val repository: NotesRepository,
) {
    suspend operator fun invoke(id: Long): Note? = repository.getNote(id)
}
