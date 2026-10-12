package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.AudioImportKind
import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.data.domain.analytics.AnalyticsTracker
import com.dmytrosamoilov.offhand.feature.recording.domain.ImportAllowance
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportAudioUseCaseTest {

    private val gate: ProUpgradeGate = mockk()
    private val observeImportAllowance: ObserveImportAllowanceUseCase = mockk {
        every { this@mockk.invoke() } returns flowOf(ImportAllowance.Unlimited)
    }
    private val userPreferences: UserPreferencesRepository = mockk(relaxed = true)
    private val controller: RecordingProcessController = mockk()
    private val sessionManager: RecordingSessionManager = mockk()
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private val useCase = ImportAudioUseCase(gate, observeImportAllowance, userPreferences, controller, sessionManager, analyticsTracker)
    private val source = AudioImportSource(handle = "/cache/imports/a", displayName = "call.m4a")
    private val second = AudioImportSource(handle = "/cache/imports/b", displayName = "talk.mp3")
    private val video = AudioImportSource(handle = "/cache/imports/c", displayName = "talk.mp4", kind = AudioImportKind.VIDEO)

    @Test
    fun `a declined paywall refuses the import without touching the pipeline`() = runTest {
        coEvery { gate.requirePro(any()) } returns false
        every { observeImportAllowance() } returns flowOf(ImportAllowance.Free(left = 3))

        assertEquals(ImportAudioResult.LOCKED, useCase(listOf(source, second)))
        verify(exactly = 0) { controller.importAudio(any()) }
        coVerify(exactly = 1) { gate.requirePro(any()) }
        coVerify(exactly = 0) { userPreferences.incrementFreeImportsUsed() }
    }

    @Test
    fun `a pro user imports every file through the process controller`() = runTest {
        every { controller.importAudio(any()) } returns true

        assertEquals(ImportAudioResult.STARTED, useCase(listOf(source, second)))
        verify { controller.importAudio(source) }
        verify { controller.importAudio(second) }
        verify(exactly = 0) { sessionManager.importAudio(any()) }
        coVerify(exactly = 0) { gate.requirePro(any()) }
        coVerify(exactly = 0) { userPreferences.incrementFreeImportsUsed() }
    }

    @Test
    fun `a free import left is spent on a single file without the paywall`() = runTest {
        every { observeImportAllowance() } returns flowOf(ImportAllowance.Free(left = 1))
        every { controller.importAudio(any()) } returns true

        assertEquals(ImportAudioResult.STARTED, useCase(listOf(video)))

        coVerify(exactly = 0) { gate.requirePro(any()) }
        coVerify(exactly = 1) { userPreferences.incrementFreeImportsUsed() }
        verify { controller.importAudio(video) }
    }

    @Test
    fun `no free import left opens the paywall`() = runTest {
        every { observeImportAllowance() } returns flowOf(ImportAllowance.Free(left = 0))
        coEvery { gate.requirePro(any()) } returns false

        assertEquals(ImportAudioResult.LOCKED, useCase(listOf(source)))
    }

    @Test
    fun `a video is only imported on its own`() = runTest {
        assertEquals(ImportAudioResult.ONE_VIDEO_AT_A_TIME, useCase(listOf(video, source)))
        verify(exactly = 0) { controller.importAudio(any()) }
    }

    @Test
    fun `falls back to in-process import when the service cannot start`() = runTest {
        every { controller.importAudio(source) } returns false
        justRun { sessionManager.importAudio(source) }

        assertEquals(ImportAudioResult.STARTED, useCase(listOf(source)))
        verify { sessionManager.importAudio(source) }
    }
}
