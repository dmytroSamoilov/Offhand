@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewPolicy
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.first

class ShouldRequestReviewUseCase(
    private val userPreferences: UserPreferencesRepository,
    private val policy: InAppReviewPolicy,
) {

    suspend operator fun invoke(): Boolean {
        val preferences = userPreferences.preferences.first()
        return policy.shouldRequestReview(
            savedRecordingsCount = preferences.savedRecordingsCount,
            state = preferences.reviewPrompt,
            nowMs = Clock.System.now().toEpochMilliseconds(),
        )
    }
}
