package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class GetNotePresetUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(): NotePreset = repository.preferences.first().notePreset
}
