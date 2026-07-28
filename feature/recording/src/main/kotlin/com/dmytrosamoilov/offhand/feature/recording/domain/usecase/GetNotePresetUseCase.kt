package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class GetNotePresetUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(): NotePreset = repository.preferences.first().notePreset
}
