package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.data.domain.SurveyRepository
import com.dmytrosamoilov.offhand.core.data.domain.SurveyState
import com.dmytrosamoilov.offhand.core.data.domain.TelemetryConsent
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferences
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.SurveyPromptPolicy
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.SurveyRules
import com.dmytrosamoilov.offhand.feature.notes.domain.survey.UsageSurvey
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveUsageSurveyPromptUseCaseTest {

    private val surveyState = MutableStateFlow(SurveyState())
    private val notes = MutableStateFlow(readyNotes(count = 3))
    private val surveyRepository: SurveyRepository = mockk {
        every { observeState(UsageSurvey.ID) } returns surveyState
    }
    private val notesRepository: NotesRepository = mockk {
        every { observeNotes() } returns notes
    }
    private val preferences = MutableStateFlow(preferencesWith(TelemetryConsent.SHARED))
    private val userPreferences: UserPreferencesRepository = mockk {
        every { preferences } returns this@ObserveUsageSurveyPromptUseCaseTest.preferences
    }
    private val observePrompt = ObserveUsageSurveyPromptUseCase(
        surveyRepository,
        notesRepository,
        userPreferences,
        SurveyPromptPolicy(SurveyRules.STANDARD),
    )

    @Test
    fun `three finished notes with sharing on ask the question`() = runTest {
        assertTrue(observePrompt().first())
    }

    @Test
    fun `notes that are not finished do not count`() = runTest {
        notes.value = readyNotes(count = 2) + note(id = 9, status = NoteStatus.PROCESSING) + note(id = 10, status = NoteStatus.FAILED)

        assertFalse(observePrompt().first())
    }

    @Test
    fun `sharing switched off or still unanswered asks nothing`() = runTest {
        preferences.value = preferencesWith(TelemetryConsent.DECLINED)
        assertFalse(observePrompt().first())

        preferences.value = preferencesWith(TelemetryConsent.UNANSWERED)
        assertFalse(observePrompt().first())
    }

    @Test
    fun `a stored answer ends the question`() = runTest {
        surveyState.value = SurveyState(answer = "meetings")

        assertFalse(observePrompt().first())
    }

    private fun readyNotes(count: Int): List<Note> = List(count) { note(id = it + 1L, status = NoteStatus.READY) }

    private fun note(id: Long, status: NoteStatus) = Note(
        id = id,
        title = "Note $id",
        body = "",
        transcript = "",
        createdAtEpochMs = id,
        transcriptionTimeMs = null,
        structuringTimeMs = null,
        hardwareBackend = null,
        status = status,
    )

    private fun preferencesWith(consent: TelemetryConsent): UserPreferences = mockk {
        every { telemetryConsent } returns consent
    }
}
