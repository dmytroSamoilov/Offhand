package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.data.domain.ReviewPromptState
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewPolicy
import com.dmytrosamoilov.offhand.feature.notes.domain.review.InAppReviewRules
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewRequestUseCasesTest {

    private val preferences = MutableStateFlow(preferences(savedRecordings = 2, notificationsPrompted = true))
    private val userPreferences: UserPreferencesRepository = mockk {
        every { preferences } returns this@ReviewRequestUseCasesTest.preferences
    }
    private val policy = InAppReviewPolicy(InAppReviewRules.ANDROID)

    @Test
    fun `a review is due once enough recordings are saved`() = runTest {
        preferences.value = preferences(savedRecordings = 3, notificationsPrompted = true)

        assertTrue(ShouldRequestReviewUseCase(userPreferences, policy)())
    }

    @Test
    fun `no review while the notification question is still open`() = runTest {
        preferences.value = preferences(savedRecordings = 3, notificationsPrompted = false)

        assertFalse(ShouldRequestReviewUseCase(userPreferences, policy)())
    }

    private fun preferences(savedRecordings: Int, notificationsPrompted: Boolean) = UserPreferences(
        onboardingCompleted = true,
        appLockEnabled = false,
        telemetryConsent = true,
        dynamicColor = false,
        developerOptions = false,
        savedRecordingsCount = savedRecordings,
        reviewPrompt = ReviewPromptState(),
        noteStyle = NoteStyleRef.DEFAULT,
        proOverride = ProOverride.STORE,
        smartSuggestionsEnabled = false,
        notificationsPrompted = notificationsPrompted,
    )
}
