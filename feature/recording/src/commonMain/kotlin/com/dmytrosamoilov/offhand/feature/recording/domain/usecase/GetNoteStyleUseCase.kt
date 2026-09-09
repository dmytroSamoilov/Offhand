package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class GetNoteStyleUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(): NoteStyleRef = repository.preferences.first().noteStyle
}
