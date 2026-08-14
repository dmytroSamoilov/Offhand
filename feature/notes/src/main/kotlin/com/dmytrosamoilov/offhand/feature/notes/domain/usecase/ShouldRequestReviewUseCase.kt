package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.feature.notes.domain.review.AppInstallInfoProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewPolicy
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class ShouldRequestReviewUseCase @Inject constructor(
    private val userPreferences: UserPreferencesRepository,
    private val installInfoProvider: AppInstallInfoProvider,
    private val policy: InAppReviewPolicy,
) {

    suspend operator fun invoke(): Boolean {
        val preferences = userPreferences.preferences.first()
        return policy.shouldRequestReview(
            savedRecordingsCount = preferences.savedRecordingsCount,
            installedAtMs = installInfoProvider.installedAtMs,
            state = preferences.reviewPrompt,
            nowMs = System.currentTimeMillis(),
        )
    }
}
