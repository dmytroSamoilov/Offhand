package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveSmartSuggestionsEnabledUseCase(
    private val repository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<Boolean> =
        repository.preferences.map { it.smartSuggestionsEnabled }.distinctUntilChanged()
}
