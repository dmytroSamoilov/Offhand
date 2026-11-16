package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteFeatures
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.domain.noteFeatures
import kotlinx.coroutines.flow.first

class GetNoteFeaturesUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(): NoteFeatures = repository.preferences.first().noteFeatures
}
