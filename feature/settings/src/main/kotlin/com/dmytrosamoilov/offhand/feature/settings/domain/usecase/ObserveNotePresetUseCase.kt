package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveNotePresetUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<NotePreset> = repository.preferences.map { it.notePreset }
}
