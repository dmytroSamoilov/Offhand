package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository

class RegisterSavedRecordingUseCase(
    private val userPreferences: UserPreferencesRepository,
) {

    suspend operator fun invoke() {
        userPreferences.incrementSavedRecordingsCount()
    }
}
