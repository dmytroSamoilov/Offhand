package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteFeatures
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.domain.noteFeatures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveNoteFeaturesUseCase(
    private val repository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<NoteFeatures> =
        repository.preferences.map { it.noteFeatures }.distinctUntilChanged()
}
