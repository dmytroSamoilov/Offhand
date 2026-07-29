package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import javax.inject.Inject

class SetNotePresetUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(preset: NotePreset) = repository.setNotePreset(preset)
}
