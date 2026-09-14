package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteSuggestionsRepository
import com.dmytrosamoilov.offhand.core.data.domain.SuggestionStatus

class UpdateSuggestionStatusUseCase(
    private val repository: NoteSuggestionsRepository,
) {
    suspend operator fun invoke(noteId: Long, index: Int, status: SuggestionStatus) =
        repository.updateStatus(noteId, index, status)
}
