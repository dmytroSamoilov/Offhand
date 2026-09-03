package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository

class SetTelemetryConsentUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(granted: Boolean) = repository.setTelemetryConsent(granted)
}
