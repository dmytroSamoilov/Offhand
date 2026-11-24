package com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.TelemetryConsent
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class GetTelemetryConsentUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(): TelemetryConsent = repository.preferences.first().telemetryConsent
}
