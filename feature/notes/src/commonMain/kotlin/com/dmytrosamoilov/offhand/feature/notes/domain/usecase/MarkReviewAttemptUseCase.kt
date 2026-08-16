@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewPolicy
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.first

class MarkReviewAttemptUseCase(
    private val userPreferences: UserPreferencesRepository,
    private val policy: InAppReviewPolicy,
) {

    suspend operator fun invoke() {
        val state = userPreferences.preferences.first().reviewPrompt
        userPreferences.setReviewPromptState(
            policy.nextStateAfterAttempt(state, Clock.System.now().toEpochMilliseconds()),
        )
    }
}
