package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Entitlements
import com.dmytrosamoilov.offhand.core.data.domain.EntitlementsRepository
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ReviewPromptState
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetNoteStyleUseCaseTest {

    private val preferences: UserPreferencesRepository = mockk()
    private val entitlements: EntitlementsRepository = mockk()
    private val useCase = GetNoteStyleUseCase(preferences, entitlements)

    @Test
    fun `custom default style is used while unlocked`() = runTest {
        every { preferences.preferences } returns flowOf(preferences(NoteStyleRef.Custom(4)))
        every { entitlements.observeEntitlements() } returns flowOf(Entitlements(customStylesUnlocked = true, audioImportUnlocked = true, calendarSuggestionsUnlocked = true, documentExportUnlocked = true))

        assertEquals(NoteStyleRef.Custom(4), useCase())
    }

    @Test
    fun `custom default style falls back to Summary when locked`() = runTest {
        every { preferences.preferences } returns flowOf(preferences(NoteStyleRef.Custom(4)))
        every { entitlements.observeEntitlements() } returns flowOf(Entitlements(customStylesUnlocked = false, audioImportUnlocked = true, calendarSuggestionsUnlocked = true, documentExportUnlocked = true))

        assertEquals(NoteStyleRef.DEFAULT, useCase())
    }

    @Test
    fun `built-in default style never consults entitlements`() = runTest {
        every { preferences.preferences } returns flowOf(preferences(NoteStyleRef.BuiltIn(NotePreset.LEGAL)))

        assertEquals(NoteStyleRef.BuiltIn(NotePreset.LEGAL), useCase())
    }

    private fun preferences(style: NoteStyleRef) = UserPreferences(
        onboardingCompleted = true,
        appLockEnabled = false,
        telemetryConsent = false,
        dynamicColor = false,
        developerOptions = false,
        savedRecordingsCount = 0,
        reviewPrompt = ReviewPromptState(),
        noteStyle = style,
    )
}
