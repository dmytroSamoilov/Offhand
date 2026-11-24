package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.device.DeviceTierProvider
import com.dmytrosamoilov.offhand.core.ai.api.ModelManager
import com.dmytrosamoilov.offhand.core.ai.api.SpeechModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsAiCoreDownloadedUseCaseTest {

    private val speechState = MutableStateFlow<SpeechModelState>(SpeechModelState.Downloaded)
    private val modelManager: ModelManager = mockk {
        coEvery { isModelDownloaded() } returns true
    }
    private val speechToText: SpeechToText = mockk {
        every { downloadState } returns speechState
    }
    private var writesSummaries = true
    private val deviceTier: DeviceTierProvider = mockk { every { writesSummaries() } answers { writesSummaries } }
    private val useCase = IsAiCoreDownloadedUseCase(modelManager, speechToText, deviceTier)

    @Test
    fun `a transcripts-only phone needs only the speech model`() = runTest {
        writesSummaries = false
        coEvery { modelManager.isModelDownloaded() } returns false

        assertTrue(useCase())
    }

    @Test
    fun `true when both models are on disk`() = runTest {
        assertTrue(useCase())
    }

    @Test
    fun `false while the speech model is missing`() = runTest {
        speechState.value = SpeechModelState.NotDownloaded

        assertFalse(useCase())
    }

    @Test
    fun `false while the language model is missing`() = runTest {
        coEvery { modelManager.isModelDownloaded() } returns false

        assertFalse(useCase())
    }
}
