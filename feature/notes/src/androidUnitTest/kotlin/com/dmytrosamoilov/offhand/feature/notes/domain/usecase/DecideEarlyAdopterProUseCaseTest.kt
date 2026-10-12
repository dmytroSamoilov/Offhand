package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.EarlyAdopterPro
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.data.domain.ReviewPromptState
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsEvents
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DecideEarlyAdopterProUseCaseTest {

    private val userPreferences: UserPreferencesRepository = mockk {
        coJustRun { setEarlyAdopterPro(any()) }
    }
    private val notesRepository: NotesRepository = mockk()
    private val proStatusRepository: ProStatusRepository = mockk {
        every { observeStatus() } returns flowOf(ProStatus.FREE)
    }
    private val analyticsTracker: AnalyticsTracker = mockk { justRun { track(any()) } }
    private val useCase = DecideEarlyAdopterProUseCase(userPreferences, notesRepository, proStatusRepository, analyticsTracker)

    @Test
    fun `an onboarded install with a note is granted once`() = runTest {
        givenPreferences(onboardingCompleted = true)
        coEvery { notesRepository.countNotes() } returns 1

        useCase()

        coVerify { userPreferences.setEarlyAdopterPro(EarlyAdopterPro.GRANTED) }
        coVerify(exactly = 0) { userPreferences.setEarlyAdopterThanked() }
        verify { analyticsTracker.track(AnalyticsEvents.proGrandfathered(hasPurchase = false)) }
    }

    @Test
    fun `a paying install is granted quietly without the thank-you`() = runTest {
        givenPreferences(onboardingCompleted = true)
        coEvery { notesRepository.countNotes() } returns 1
        every { proStatusRepository.observeStatus() } returns flowOf(ProStatus.LIFETIME)
        coJustRun { userPreferences.setEarlyAdopterThanked() }

        useCase()

        coVerify { userPreferences.setEarlyAdopterPro(EarlyAdopterPro.GRANTED) }
        coVerify { userPreferences.setEarlyAdopterThanked() }
        verify { analyticsTracker.track(AnalyticsEvents.proGrandfathered(hasPurchase = true)) }
    }

    @Test
    fun `an onboarded install without notes gets the free tier`() = runTest {
        givenPreferences(onboardingCompleted = true)
        coEvery { notesRepository.countNotes() } returns 0

        useCase()

        coVerify { userPreferences.setEarlyAdopterPro(EarlyAdopterPro.NONE) }
        verify(exactly = 0) { analyticsTracker.track(any()) }
    }

    @Test
    fun `a fresh install is settled without opening the database`() = runTest {
        givenPreferences(onboardingCompleted = false)

        useCase()

        coVerify { userPreferences.setEarlyAdopterPro(EarlyAdopterPro.NONE) }
        coVerify(exactly = 0) { notesRepository.countNotes() }
    }

    @Test
    fun `a settled install is never re-decided`() = runTest {
        givenPreferences(onboardingCompleted = true, decision = EarlyAdopterPro.GRANTED)

        useCase()

        coVerify(exactly = 0) { userPreferences.setEarlyAdopterPro(any()) }
        coVerify(exactly = 0) { notesRepository.countNotes() }
    }

    private fun givenPreferences(onboardingCompleted: Boolean, decision: EarlyAdopterPro = EarlyAdopterPro.UNDECIDED) {
        every { userPreferences.preferences } returns flowOf(
            UserPreferences(
                onboardingCompleted = onboardingCompleted,
                appLockEnabled = false,
                telemetryConsent = false,
                dynamicColor = false,
                developerOptions = false,
                savedRecordingsCount = 0,
                reviewPrompt = ReviewPromptState(),
                noteStyle = NoteStyleRef.DEFAULT,
                proOverride = ProOverride.STORE,
                smartSuggestionsEnabled = false,
                earlyAdopterPro = decision,
            ),
        )
    }
}
