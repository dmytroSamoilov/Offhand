package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.first

// Keeps the percent of an unfinished on-device AI download across process
// starts; a value still there at the next cold start means the download was
// abandoned.
class RememberModelDownloadProgressUseCase(
    private val userPreferences: UserPreferencesRepository,
) {
    suspend fun pending(): Int? = userPreferences.preferences.first().modelDownloadProgress

    suspend fun remember(percent: Int?) = userPreferences.setModelDownloadProgress(percent)
}
