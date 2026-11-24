package com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository

class AnswerTelemetryConsentUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(isShared: Boolean) = repository.setTelemetryConsent(isShared)
}
