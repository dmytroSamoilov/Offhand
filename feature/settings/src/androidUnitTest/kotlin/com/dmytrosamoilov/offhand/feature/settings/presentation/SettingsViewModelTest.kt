package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveNotePresetUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetNotePresetUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val setDynamicColor: SetDynamicColorUseCase = mockk(relaxed = true)
    private val observeDynamicColor: ObserveDynamicColorUseCase = mockk()
    private val setNotePreset: SetNotePresetUseCase = mockk(relaxed = true)
    private val observeNotePreset: ObserveNotePresetUseCase = mockk()
    private val setAppLockEnabled: SetAppLockEnabledUseCase = mockk(relaxed = true)
    private val observeAppLockEnabled: ObserveAppLockEnabledUseCase = mockk()
    private val appLockManager: AppLockManager = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { observeDynamicColor() } returns flowOf(true)
        every { observeNotePreset() } returns flowOf(NotePreset.MEETING)
        every { observeAppLockEnabled() } returns flowOf(true)
        every { appLockManager.isDeviceSecure } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(
        observeDynamicColor = observeDynamicColor,
        setDynamicColor = setDynamicColor,
        observeNotePreset = observeNotePreset,
        setNotePreset = setNotePreset,
        observeAppLockEnabled = observeAppLockEnabled,
        setAppLockEnabled = setAppLockEnabled,
        appLockManager = appLockManager,
    )

    @Test
    fun `state reflects note preset and dynamic color`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(NotePreset.MEETING, viewModel.uiState.value.notePreset)
        assertTrue(viewModel.uiState.value.isDynamicColorEnabled)
    }

    @Test
    fun `note preset selection persists the preference`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onNotePresetSelected(NotePreset.VISIT)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setNotePreset(NotePreset.VISIT) }
    }

    @Test
    fun `dynamic color toggle persists preference`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDynamicColorChanged(false)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setDynamicColor(false) }
    }

    @Test
    fun `app lock toggle persists preference`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAppLockEnabled)
        viewModel.onAppLockChanged(false)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setAppLockEnabled(false) }
    }

    @Test
    fun `app lock cannot be enabled without a device passcode`() = runTest(dispatcher) {
        every { appLockManager.isDeviceSecure } returns false
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAppLockChanged(true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isDeviceSecure)
        coVerify { setAppLockEnabled(false) }
    }
}
