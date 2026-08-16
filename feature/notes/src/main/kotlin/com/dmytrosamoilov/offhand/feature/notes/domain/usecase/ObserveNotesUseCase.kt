package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import kotlinx.coroutines.flow.Flow

class ObserveNotesUseCase(
    private val repository: NotesRepository,
) {
    operator fun invoke(): Flow<List<Note>> = repository.observeNotes()
}
