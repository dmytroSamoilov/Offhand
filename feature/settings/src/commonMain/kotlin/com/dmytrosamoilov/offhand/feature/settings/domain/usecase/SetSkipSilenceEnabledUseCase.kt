package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository

class SetSkipSilenceEnabledUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setSkipSilenceEnabled(enabled)
}
