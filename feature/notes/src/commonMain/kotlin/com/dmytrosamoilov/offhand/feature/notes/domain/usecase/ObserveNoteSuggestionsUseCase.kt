package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteSuggestions
import com.dmytrosamoilov.offhand.core.data.domain.NoteSuggestionsRepository
import kotlinx.coroutines.flow.Flow

class ObserveNoteSuggestionsUseCase(
    private val repository: NoteSuggestionsRepository,
) {
    operator fun invoke(noteId: Long): Flow<NoteSuggestions?> = repository.observeSuggestions(noteId)
}
