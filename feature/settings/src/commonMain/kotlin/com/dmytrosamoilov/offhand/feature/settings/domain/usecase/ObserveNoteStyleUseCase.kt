package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveNoteStyleUseCase(
    private val repository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<NoteStyleRef> = repository.preferences.map { it.noteStyle }
}
