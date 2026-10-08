package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveNotificationsPromptedUseCase(
    private val userPreferences: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<Boolean> = userPreferences.preferences.map { it.notificationsPrompted }
}
