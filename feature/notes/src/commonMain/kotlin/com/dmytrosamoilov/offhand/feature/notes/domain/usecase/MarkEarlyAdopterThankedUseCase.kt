package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository

class MarkEarlyAdopterThankedUseCase(
    private val userPreferences: UserPreferencesRepository,
) {
    suspend operator fun invoke() = userPreferences.setEarlyAdopterThanked()
}
