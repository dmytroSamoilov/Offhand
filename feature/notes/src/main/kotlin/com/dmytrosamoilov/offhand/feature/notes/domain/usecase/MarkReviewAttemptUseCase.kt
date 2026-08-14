package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewPolicy
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class MarkReviewAttemptUseCase @Inject constructor(
    private val userPreferences: UserPreferencesRepository,
    private val policy: InAppReviewPolicy,
) {

    suspend operator fun invoke() {
        val state = userPreferences.preferences.first().reviewPrompt
        userPreferences.setReviewPromptState(
            policy.nextStateAfterAttempt(state, System.currentTimeMillis()),
        )
    }
}
