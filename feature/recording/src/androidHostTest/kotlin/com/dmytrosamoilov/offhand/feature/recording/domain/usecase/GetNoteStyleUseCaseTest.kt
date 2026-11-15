package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.ProStatus
import com.dmytrosamoilov.offhand.core.data.domain.ProStatusRepository
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProOverride
import com.dmytrosamoilov.offhand.core.data.domain.ReviewPromptState
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetNoteStyleUseCaseTest {

    private val preferences: UserPreferencesRepository = mockk()
    private val entitlements: ProStatusRepository = mockk()
    private val getFolderStyle: GetFolderStyleUseCase = mockk {
        coEvery { this@mockk.invoke(any()) } returns null
    }
    private val useCase = GetNoteStyleUseCase(preferences, entitlements, getFolderStyle)

    @Test
    fun `custom default style is used while unlocked`() = runTest {
        every { preferences.preferences } returns flowOf(preferences(NoteStyleRef.Custom(4)))
        every { entitlements.observeStatus() } returns flowOf(ProStatus.LIFETIME)

        assertEquals(NoteStyleRef.Custom(4), useCase())
    }

    @Test
    fun `custom default style falls back to Summary when locked`() = runTest {
        every { preferences.preferences } returns flowOf(preferences(NoteStyleRef.Custom(4)))
        every { entitlements.observeStatus() } returns flowOf(ProStatus.FREE)

        assertEquals(NoteStyleRef.DEFAULT, useCase())
    }

    @Test
    fun `built-in default style never consults entitlements`() = runTest {
        every { preferences.preferences } returns flowOf(preferences(NoteStyleRef.BuiltIn(NotePreset.LEGAL)))

        assertEquals(NoteStyleRef.BuiltIn(NotePreset.LEGAL), useCase())
    }

    @Test
    fun `a folder style wins over the default`() = runTest {
        every { preferences.preferences } returns flowOf(preferences(NoteStyleRef.BuiltIn(NotePreset.LEGAL)))
        coEvery { getFolderStyle(7L) } returns NoteStyleRef.BuiltIn(NotePreset.MEETING)

        assertEquals(NoteStyleRef.BuiltIn(NotePreset.MEETING), useCase(folderId = 7L))
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
        proOverride = ProOverride.STORE,
        smartSuggestionsEnabled = false,
    )
}
