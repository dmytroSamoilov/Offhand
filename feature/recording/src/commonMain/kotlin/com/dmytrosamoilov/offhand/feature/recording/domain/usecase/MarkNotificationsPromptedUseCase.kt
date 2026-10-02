package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository

class MarkNotificationsPromptedUseCase(
    private val userPreferences: UserPreferencesRepository,
) {
    suspend operator fun invoke() = userPreferences.setNotificationsPrompted()
}
