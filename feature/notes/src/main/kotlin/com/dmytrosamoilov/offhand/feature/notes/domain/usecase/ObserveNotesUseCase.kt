package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveNotesUseCase @Inject constructor(
    private val repository: NotesRepository,
) {
    operator fun invoke(): Flow<List<Note>> = repository.observeNotes()
}
