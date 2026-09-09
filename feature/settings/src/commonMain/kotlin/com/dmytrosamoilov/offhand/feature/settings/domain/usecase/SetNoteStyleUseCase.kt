package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository

class SetNoteStyleUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(style: NoteStyleRef) = repository.setNoteStyle(style)
}
