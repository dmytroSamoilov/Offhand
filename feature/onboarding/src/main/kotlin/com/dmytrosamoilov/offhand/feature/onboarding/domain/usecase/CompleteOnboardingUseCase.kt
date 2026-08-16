package com.dmytrosamoilov.offhand.feature.onboarding.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository

class CompleteOnboardingUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke() = repository.setOnboardingCompleted(true)
}
