package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository

class SetSmartSuggestionsEnabledUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setSmartSuggestionsEnabled(enabled)
}
