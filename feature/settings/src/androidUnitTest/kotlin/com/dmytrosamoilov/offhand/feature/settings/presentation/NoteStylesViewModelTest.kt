package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.IsCustomNoteStylesAvailableUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveCustomNoteStylesUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetNoteStyleUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class NoteStylesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val observeCustomNoteStyles: ObserveCustomNoteStylesUseCase = mockk()
    private val isCustomNoteStylesAvailable: IsCustomNoteStylesAvailableUseCase = mockk()
    private val observeNoteStyle: ObserveNoteStyleUseCase = mockk()
    private val setNoteStyle: SetNoteStyleUseCase = mockk(relaxed = true)
    private val gate: ProUpgradeGate = mockk()
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)

    private val customStyle = CustomNoteStyle(
        id = 1,
        name = "Debrief",
        noteKind = "a sales call debrief",
        language = NoteStyleLanguage.ENGLISH,
        sections = emptyList(),
        createdAtEpochMs = 0,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { observeCustomNoteStyles() } returns flowOf(listOf(customStyle))
        every { isCustomNoteStylesAvailable() } returns flowOf(true)
        every { observeNoteStyle() } returns flowOf(NoteStyleRef.BuiltIn(NotePreset.MEETING))
        coEvery { gate.requirePro(any()) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = NoteStylesViewModel(
        observeCustomNoteStyles = observeCustomNoteStyles,
        isCustomNoteStylesAvailable = isCustomNoteStylesAvailable,
        observeNoteStyle = observeNoteStyle,
        setNoteStyle = setNoteStyle,
        proUpgradeGate = gate,
        analyticsTracker = analyticsTracker,
    )

    @Test
    fun `state carries the default style next to the custom styles`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(NoteStyleRef.BuiltIn(NotePreset.MEETING), viewModel.uiState.value.selected)
        assertEquals(listOf("Debrief"), viewModel.uiState.value.customStyles.map { it.name })
    }

    @Test
    fun `selecting a built-in style persists the default`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onStyleSelected(NoteStyleRef.BuiltIn(NotePreset.VISIT))
        advanceUntilIdle()

        coVerify { setNoteStyle(NoteStyleRef.BuiltIn(NotePreset.VISIT)) }
    }

    @Test
    fun `custom styles stay listed while locked and selecting one goes through the paywall`() = runTest(dispatcher) {
        every { isCustomNoteStylesAvailable() } returns flowOf(false)
        coEvery { gate.requirePro(any()) } returns false
        val viewModel = viewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUnlocked)
        viewModel.onStyleSelected(NoteStyleRef.Custom(1))
        advanceUntilIdle()

        coVerify(exactly = 0) { setNoteStyle(any()) }
    }
}
