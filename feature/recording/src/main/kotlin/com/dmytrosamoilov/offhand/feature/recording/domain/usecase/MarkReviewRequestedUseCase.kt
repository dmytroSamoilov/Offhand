package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import javax.inject.Inject

class MarkReviewRequestedUseCase @Inject constructor(
    private val userPreferences: UserPreferencesRepository,
) {

    suspend operator fun invoke() {
        userPreferences.setLastReviewRequestAt(System.currentTimeMillis())
    }
}
