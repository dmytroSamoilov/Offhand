package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStyle
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleLanguage
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleSection
import com.dmytrosamoilov.offhand.core.data.domain.SectionFormat
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ImportAudioResult
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ImportAudioUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAudioImportAvailableUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveCustomNoteStylesUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.IsCustomNoteStylesAvailableUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.ObserveNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetAppLockEnabledUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetDynamicColorUseCase
import com.dmytrosamoilov.offhand.feature.settings.domain.usecase.SetNoteStyleUseCase
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
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val setDynamicColor: SetDynamicColorUseCase = mockk(relaxed = true)
    private val observeDynamicColor: ObserveDynamicColorUseCase = mockk()
    private val setNoteStyle: SetNoteStyleUseCase = mockk(relaxed = true)
    private val observeNoteStyle: ObserveNoteStyleUseCase = mockk()
    private val observeCustomNoteStyles: ObserveCustomNoteStylesUseCase = mockk()
    private val isCustomNoteStylesAvailable: IsCustomNoteStylesAvailableUseCase = mockk()
    private val setAppLockEnabled: SetAppLockEnabledUseCase = mockk(relaxed = true)
    private val observeAppLockEnabled: ObserveAppLockEnabledUseCase = mockk()
    private val appLockManager: AppLockManager = mockk()
    private val importAudio: ImportAudioUseCase = mockk()
    private val isAudioImportAvailable: IsAudioImportAvailableUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { observeDynamicColor() } returns flowOf(true)
        every { observeNoteStyle() } returns flowOf(NoteStyleRef.BuiltIn(NotePreset.MEETING))
        every { observeCustomNoteStyles() } returns flowOf(emptyList())
        every { isCustomNoteStylesAvailable() } returns flowOf(true)
        every { observeAppLockEnabled() } returns flowOf(true)
        every { appLockManager.isDeviceSecure } returns true
        every { isAudioImportAvailable() } returns flowOf(true)
        coEvery { importAudio(any()) } returns ImportAudioResult.STARTED
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(
        observeDynamicColor = observeDynamicColor,
        setDynamicColor = setDynamicColor,
        observeNoteStyle = observeNoteStyle,
        setNoteStyle = setNoteStyle,
        observeCustomNoteStyles = observeCustomNoteStyles,
        isCustomNoteStylesAvailable = isCustomNoteStylesAvailable,
        observeAppLockEnabled = observeAppLockEnabled,
        setAppLockEnabled = setAppLockEnabled,
        appLockManager = appLockManager,
        importAudio = importAudio,
        isAudioImportAvailable = isAudioImportAvailable,
    )

    @Test
    fun `state reflects note preset and dynamic color`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(NoteStyleRef.BuiltIn(NotePreset.MEETING), viewModel.uiState.value.noteStyle)
        assertTrue(viewModel.uiState.value.isDynamicColorEnabled)
    }

    @Test
    fun `note preset selection persists the preference`() = runTest(dispatcher) {
        val viewModel = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onNoteStyleSelected(NoteStyleRef.BuiltIn(NotePreset.VISIT))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { setNoteStyle(NoteStyleRef.BuiltIn(NotePreset.VISIT)) }
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

    @Test
    fun `custom styles are hidden from the default picker while locked`() = runTest(dispatcher) {
        every { observeCustomNoteStyles() } returns flowOf(listOf(customStyle))
        every { isCustomNoteStylesAvailable() } returns flowOf(false)
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.customStyles.isEmpty())
        assertFalse(viewModel.uiState.value.isCustomStylesUnlocked)
    }

    @Test
    fun `custom styles are listed once unlocked`() = runTest(dispatcher) {
        every { observeCustomNoteStyles() } returns flowOf(listOf(customStyle))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(listOf("Debrief"), viewModel.uiState.value.customStyles.map { it.name })
    }

    private val customStyle = CustomNoteStyle(
        id = 1,
        name = "Debrief",
        noteKind = "",
        language = NoteStyleLanguage.RECORDING,
        sections = listOf(NoteStyleSection("Customer", "", SectionFormat.SENTENCES)),
        createdAtEpochMs = 0,
    )

    @Test
    fun `importing several files starts each one and reports the count`() = runTest {
        val viewModel = viewModel()
        val sources = listOf(AudioImportSource("/a", "a.m4a"), AudioImportSource("/b", "b.mp3"))

        viewModel.onAudioImportSelected(sources, unreadableCount = 0)
        advanceUntilIdle()

        assertEquals(ImportNoticeUi.Started(2), viewModel.uiState.value.importNotice)
        coVerify { importAudio(sources[0]) }
        coVerify { importAudio(sources[1]) }
        viewModel.onImportNoticeDismissed()
        assertEquals(null, viewModel.uiState.value.importNotice)
    }

    @Test
    fun `a locked entitlement wins over unreadable files and a started import`() = runTest {
        coEvery { importAudio(any()) } returns ImportAudioResult.LOCKED
        val viewModel = viewModel()

        viewModel.onAudioImportSelected(listOf(AudioImportSource("/a", "a.m4a")), unreadableCount = 1)
        advanceUntilIdle()

        assertEquals(ImportNoticeUi.Locked, viewModel.uiState.value.importNotice)
    }

    @Test
    fun `unreadable files are reported when nothing was locked`() = runTest {
        val viewModel = viewModel()

        viewModel.onAudioImportSelected(listOf(AudioImportSource("/a", "a.m4a")), unreadableCount = 1)
        advanceUntilIdle()

        assertEquals(ImportNoticeUi.Unreadable, viewModel.uiState.value.importNotice)
        coVerify(exactly = 1) { importAudio(any()) }
    }
}
