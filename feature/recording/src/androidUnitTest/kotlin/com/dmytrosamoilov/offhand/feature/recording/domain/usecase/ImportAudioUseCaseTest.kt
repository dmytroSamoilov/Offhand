package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.core.data.domain.ProUpgradeGate
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportAudioUseCaseTest {

    private val gate: ProUpgradeGate = mockk()
    private val controller: RecordingProcessController = mockk()
    private val sessionManager: RecordingSessionManager = mockk()
    private val useCase = ImportAudioUseCase(gate, controller, sessionManager)
    private val source = AudioImportSource(handle = "/cache/imports/a", displayName = "call.m4a")
    private val second = AudioImportSource(handle = "/cache/imports/b", displayName = "talk.mp3")

    @Test
    fun `a declined paywall refuses the import without touching the pipeline`() = runTest {
        coEvery { gate.requirePro(any()) } returns false

        assertEquals(ImportAudioResult.LOCKED, useCase(listOf(source, second)))
        verify(exactly = 0) { controller.importAudio(any()) }
        coVerify(exactly = 1) { gate.requirePro(any()) }
    }

    @Test
    fun `a pro user imports every file through the process controller`() = runTest {
        coEvery { gate.requirePro(any()) } returns true
        every { controller.importAudio(any()) } returns true

        assertEquals(ImportAudioResult.STARTED, useCase(listOf(source, second)))
        verify { controller.importAudio(source) }
        verify { controller.importAudio(second) }
        verify(exactly = 0) { sessionManager.importAudio(any()) }
    }

    @Test
    fun `falls back to in-process import when the service cannot start`() = runTest {
        coEvery { gate.requirePro(any()) } returns true
        every { controller.importAudio(source) } returns false
        justRun { sessionManager.importAudio(source) }

        assertEquals(ImportAudioResult.STARTED, useCase(listOf(source)))
        verify { sessionManager.importAudio(source) }
    }
}
