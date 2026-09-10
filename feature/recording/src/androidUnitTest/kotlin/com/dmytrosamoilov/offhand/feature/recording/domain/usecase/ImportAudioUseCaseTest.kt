package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.core.data.domain.Entitlements
import com.dmytrosamoilov.offhand.core.data.domain.EntitlementsRepository
import com.dmytrosamoilov.offhand.core.data.domain.RecordingProcessController
import com.dmytrosamoilov.offhand.feature.recording.domain.RecordingSessionManager
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportAudioUseCaseTest {

    private val entitlements: EntitlementsRepository = mockk()
    private val controller: RecordingProcessController = mockk()
    private val sessionManager: RecordingSessionManager = mockk()
    private val useCase = ImportAudioUseCase(entitlements, controller, sessionManager)
    private val source = AudioImportSource(handle = "/cache/imports/a", displayName = "call.m4a")

    @Test
    fun `locked entitlement refuses the import without touching the pipeline`() = runTest {
        every { entitlements.observeEntitlements() } returns flowOf(Entitlements(customStylesUnlocked = true, audioImportUnlocked = false, calendarSuggestionsUnlocked = true))

        assertEquals(ImportAudioResult.LOCKED, useCase(source))
        verify(exactly = 0) { controller.importAudio(any()) }
    }

    @Test
    fun `unlocked import goes through the process controller`() = runTest {
        every { entitlements.observeEntitlements() } returns flowOf(Entitlements(customStylesUnlocked = true, audioImportUnlocked = true, calendarSuggestionsUnlocked = true))
        every { controller.importAudio(source) } returns true

        assertEquals(ImportAudioResult.STARTED, useCase(source))
        verify(exactly = 0) { sessionManager.importAudio(any()) }
    }

    @Test
    fun `falls back to in-process import when the service cannot start`() = runTest {
        every { entitlements.observeEntitlements() } returns flowOf(Entitlements(customStylesUnlocked = true, audioImportUnlocked = true, calendarSuggestionsUnlocked = true))
        every { controller.importAudio(source) } returns false
        justRun { sessionManager.importAudio(source) }

        assertEquals(ImportAudioResult.STARTED, useCase(source))
        verify { sessionManager.importAudio(source) }
    }
}
