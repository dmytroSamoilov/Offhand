package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteFeature
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository

class SetNoteFeatureEnabledUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(feature: NoteFeature, enabled: Boolean) = repository.setNoteFeatureEnabled(feature, enabled)
}
