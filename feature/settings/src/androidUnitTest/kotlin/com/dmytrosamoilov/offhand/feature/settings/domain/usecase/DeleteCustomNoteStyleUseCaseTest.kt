package com.dmytrosamoilov.offhand.feature.settings.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStylesRepository
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ReviewPromptState
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteCustomNoteStyleUseCaseTest {

    private val repository: CustomNoteStylesRepository = mockk { coJustRun { deleteStyle(any()) } }
    private val preferences: UserPreferencesRepository = mockk { coJustRun { setNoteStyle(any()) } }
    private val useCase = DeleteCustomNoteStyleUseCase(repository, preferences)

    @Test
    fun `deleting the default style resets the preference first`() = runTest {
        every { preferences.preferences } returns flowOf(preferences(NoteStyleRef.Custom(5)))

        useCase(5)

        coVerify { preferences.setNoteStyle(NoteStyleRef.DEFAULT) }
        coVerify { repository.deleteStyle(5) }
    }

    @Test
    fun `deleting another style leaves the preference alone`() = runTest {
        every { preferences.preferences } returns flowOf(preferences(NoteStyleRef.BuiltIn(NotePreset.MEETING)))

        useCase(5)

        coVerify(exactly = 0) { preferences.setNoteStyle(any()) }
        coVerify { repository.deleteStyle(5) }
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
