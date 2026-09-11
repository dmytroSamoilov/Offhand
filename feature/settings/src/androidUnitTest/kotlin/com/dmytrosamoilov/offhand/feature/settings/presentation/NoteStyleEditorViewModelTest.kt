package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleDraft
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleDraftException
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleSection
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.SectionFormat
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleErrors
import com.dmytrosamoilov.offhand.feature.settings.domain.NoteStyleFieldError
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.DraftNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.GetCustomNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.IsCustomNoteStylesAvailableUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SaveCustomNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SaveNoteStyleResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteStyleEditorViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val getStyle: GetCustomNoteStyleUseCase = mockk()
    private val saveStyle: SaveCustomNoteStyleUseCase = mockk()
    private val draftStyle: DraftNoteStyleUseCase = mockk()
    private val isAvailable: IsCustomNoteStylesAvailableUseCase = mockk {
        every { this@mockk.invoke() } returns flowOf(true)
    }
    private val gate: ProUpgradeGate = mockk {
        coEvery { requirePro(any()) } returns true
    }

    private val stored = CustomNoteStyle(
        id = 3,
        name = "Visit",
        noteKind = "a visit",
        language = NoteStyleLanguage.ENGLISH,
        sections = listOf(
            NoteStyleSection("Who", "", SectionFormat.SENTENCES),
            NoteStyleSection("Plan", "", SectionFormat.BULLETS),
        ),
        createdAtEpochMs = 7,
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(styleId: Long) = NoteStyleEditorViewModel(styleId, getStyle, saveStyle, draftStyle, isAvailable, gate)

    @Test
    fun `new editor starts with one empty section`() {
        val state = viewModel(0L).uiState.value

        assertTrue(state.isNew)
        assertEquals(listOf(SectionDraftUi()), state.sections)
    }

    @Test
    fun `existing style is loaded into the form`() = runTest(dispatcher) {
        coEvery { getStyle(3L) } returns stored
        val viewModel = viewModel(3L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isNew)
        assertEquals("Visit", state.name)
        assertEquals(NoteStyleLanguage.ENGLISH, state.language)
        assertEquals(listOf("Who", "Plan"), state.sections.map { it.heading })
    }

    @Test
    fun `sections can be added, moved and removed`() {
        val viewModel = viewModel(0L)
        viewModel.onSectionHeadingChanged(0, "A")
        viewModel.onSectionAdded()
        viewModel.onSectionHeadingChanged(1, "B")

        viewModel.onSectionMoved(1, 0)
        assertEquals(listOf("B", "A"), viewModel.uiState.value.sections.map { it.heading })

        viewModel.onSectionRemoved(0)
        assertEquals(listOf("A"), viewModel.uiState.value.sections.map { it.heading })
    }

    @Test
    fun `save keeps the stored id and created timestamp and flags completion`() = runTest(dispatcher) {
        coEvery { getStyle(3L) } returns stored
        coEvery { saveStyle(any()) } returns SaveNoteStyleResult.Saved(3L)
        val viewModel = viewModel(3L)
        advanceUntilIdle()
        viewModel.onNameChanged("Visit report")

        viewModel.onSaveRequested()
        advanceUntilIdle()

        coVerify { saveStyle(stored.copy(name = "Visit report")) }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `validation errors are shown and cleared when the field changes`() = runTest(dispatcher) {
        coEvery { saveStyle(any()) } returns SaveNoteStyleResult.Invalid(NoteStyleErrors(name = NoteStyleFieldError.BLANK))
        val viewModel = viewModel(0L)

        viewModel.onSaveRequested()
        advanceUntilIdle()
        assertEquals(NoteStyleFieldError.BLANK, viewModel.uiState.value.errors.name)

        viewModel.onNameChanged("Named")
        assertEquals(null, viewModel.uiState.value.errors.name)
    }

    @Test
    fun `describing the note fills the form from the draft and closes the dialog`() = runTest(dispatcher) {
        coEvery { draftStyle("football training recap") } returns NoteStyleDraft(
            name = "Training recap",
            noteKind = "a training recap",
            sections = listOf(NoteStyleSection("Drills", "what was practised", SectionFormat.BULLETS)),
        )
        val viewModel = viewModel(0L)
        viewModel.onDescribeRequested()
        viewModel.onDescriptionChanged("football training recap")

        viewModel.onDraftRequested()
        assertEquals(DescribeStatusUi.RUNNING, viewModel.uiState.value.describe?.status)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(null, state.describe)
        assertEquals("Training recap", state.name)
        assertEquals("a training recap", state.noteKind)
        assertEquals(listOf(SectionDraftUi("Drills", "what was practised", SectionFormat.BULLETS)), state.sections)
    }

    @Test
    fun `an unusable answer keeps the dialog open with a failure`() = runTest(dispatcher) {
        coEvery { draftStyle(any()) } throws NoteStyleDraftException()
        val viewModel = viewModel(0L)
        viewModel.onDescribeRequested()
        viewModel.onDescriptionChanged("something")

        viewModel.onDraftRequested()
        advanceUntilIdle()

        assertEquals(DescribeStatusUi.FAILED, viewModel.uiState.value.describe?.status)
        assertEquals("", viewModel.uiState.value.name)
    }

    @Test
    fun `describing without a model explains why`() = runTest(dispatcher) {
        coEvery { draftStyle(any()) } returns null
        val viewModel = viewModel(0L)
        viewModel.onDescribeRequested()
        viewModel.onDescriptionChanged("something")

        viewModel.onDraftRequested()
        advanceUntilIdle()

        assertEquals(DescribeStatusUi.MODEL_UNAVAILABLE, viewModel.uiState.value.describe?.status)
    }

    @Test
    fun `a locked editor keeps the draft and refuses to save when the paywall is declined`() = runTest(dispatcher) {
        every { isAvailable() } returns flowOf(false)
        coEvery { gate.requirePro(any()) } returns false
        val viewModel = viewModel(0L)
        advanceUntilIdle()
        viewModel.onNameChanged("Debrief")
        viewModel.onSectionHeadingChanged(0, "A")

        viewModel.onSaveRequested()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLocked)
        assertFalse(viewModel.uiState.value.isSaved)
        assertEquals("Debrief", viewModel.uiState.value.name)
        coVerify(exactly = 0) { saveStyle(any()) }
    }

    @Test
    fun `an invalid draft never reaches the paywall`() = runTest(dispatcher) {
        every { isAvailable() } returns flowOf(false)
        val viewModel = viewModel(0L)
        advanceUntilIdle()

        viewModel.onSaveRequested()
        advanceUntilIdle()

        coVerify(exactly = 0) { gate.requirePro(any()) }
        coVerify(exactly = 0) { saveStyle(any()) }
    }
}
