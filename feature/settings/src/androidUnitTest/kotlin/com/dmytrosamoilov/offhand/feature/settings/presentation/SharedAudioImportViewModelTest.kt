package com.dmytrosamoilov.offhand.feature.settings.presentation

import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.DiscardStagedAudioUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ImportAudioResult
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.ImportAudioUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAudioImportAvailableUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedAudioImportViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val importAudio: ImportAudioUseCase = mockk()
    private val discardStagedAudio: DiscardStagedAudioUseCase = mockk(relaxed = true)
    private val isAudioImportAvailable: IsAudioImportAvailableUseCase = mockk()
    private val sources = listOf(AudioImportSource(handle = "/tmp/a", displayName = "a.m4a"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { importAudio(any()) } returns ImportAudioResult.STARTED
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pro user imports at once and sees the started notice`() = runTest(dispatcher) {
        every { isAudioImportAvailable() } returns flowOf(true)
        val viewModel = SharedAudioImportViewModel(importAudio, discardStagedAudio, isAudioImportAvailable)

        viewModel.onSharedAudioReceived(sources, unreadableCount = 0)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isProDialogShown)
        assertEquals(ImportNoticeUi.Started(1), viewModel.uiState.value.notice)
        coVerify(exactly = 1) { importAudio(sources) }
    }

    @Test
    fun `free user is asked first and nothing is imported yet`() = runTest(dispatcher) {
        every { isAudioImportAvailable() } returns flowOf(false)
        val viewModel = SharedAudioImportViewModel(importAudio, discardStagedAudio, isAudioImportAvailable)

        viewModel.onSharedAudioReceived(sources, unreadableCount = 0)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isProDialogShown)
        assertNull(viewModel.uiState.value.notice)
        coVerify(exactly = 0) { importAudio(any()) }
    }

    @Test
    fun `upgrade runs the gated import with the pending files`() = runTest(dispatcher) {
        every { isAudioImportAvailable() } returns flowOf(false)
        val viewModel = SharedAudioImportViewModel(importAudio, discardStagedAudio, isAudioImportAvailable)
        viewModel.onSharedAudioReceived(sources, unreadableCount = 0)
        advanceUntilIdle()

        viewModel.onUpgradeClicked()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isProDialogShown)
        assertEquals(ImportNoticeUi.Started(1), viewModel.uiState.value.notice)
        coVerify(exactly = 1) { importAudio(sources) }
    }

    @Test
    fun `declined paywall after upgrade discards the staged files`() = runTest(dispatcher) {
        every { isAudioImportAvailable() } returns flowOf(false)
        coEvery { importAudio(any()) } returns ImportAudioResult.LOCKED
        val viewModel = SharedAudioImportViewModel(importAudio, discardStagedAudio, isAudioImportAvailable)
        viewModel.onSharedAudioReceived(sources, unreadableCount = 0)
        advanceUntilIdle()

        viewModel.onUpgradeClicked()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.notice)
        coVerify(exactly = 1) { discardStagedAudio(sources) }
    }

    @Test
    fun `cancel discards the staged files without importing`() = runTest(dispatcher) {
        every { isAudioImportAvailable() } returns flowOf(false)
        val viewModel = SharedAudioImportViewModel(importAudio, discardStagedAudio, isAudioImportAvailable)
        viewModel.onSharedAudioReceived(sources, unreadableCount = 0)
        advanceUntilIdle()

        viewModel.onImportDeclined()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isProDialogShown)
        coVerify(exactly = 1) { discardStagedAudio(sources) }
        coVerify(exactly = 0) { importAudio(any()) }
    }

    @Test
    fun `unreadable files alone show the notice without the pro dialog`() = runTest(dispatcher) {
        every { isAudioImportAvailable() } returns flowOf(false)
        val viewModel = SharedAudioImportViewModel(importAudio, discardStagedAudio, isAudioImportAvailable)

        viewModel.onSharedAudioReceived(emptyList(), unreadableCount = 2)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isProDialogShown)
        assertEquals(ImportNoticeUi.Unreadable, viewModel.uiState.value.notice)
    }
}
