package com.dmytrosamoilov.offhand.core.ai.api

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AiCoreDownloadStatusTest {

    private val modelState = MutableStateFlow<ModelState>(ModelState.Ready)
    private val speechState = MutableStateFlow<SpeechModelState>(SpeechModelState.Downloaded)

    private val modelManager: ModelManager = mockk {
        every { this@mockk.modelState } returns this@AiCoreDownloadStatusTest.modelState
        every { model } returns mockk<AvailableModel> {
            every { sizeInBytes } returns 3_000L
        }
        every { speechModelSizeInBytes } returns 1_000L
    }
    private val speechToText: SpeechToText = mockk {
        every { downloadState } returns speechState
    }

    private val status = AiCoreDownloadStatus(modelManager, speechToText)

    @Test
    fun `idle when nothing is downloading`() = runTest {
        assertEquals(AiCoreDownloadState.Idle, status.state.first())
    }

    @Test
    fun `speech download counts against the combined total`() = runTest {
        modelState.value = ModelState.NotDownloaded
        speechState.value = SpeechModelState.Downloading(bytesDownloaded = 500, bytesTotal = 1_000)

        assertEquals(AiCoreDownloadState.Downloading(progressPercent = 12), status.state.first())
    }

    @Test
    fun `model download continues from completed speech bytes`() = runTest {
        modelState.value = ModelState.Downloading(
            progress = 0.5f,
            bytesDownloaded = 1_500,
            bytesTotal = 3_000,
        )
        speechState.value = SpeechModelState.Downloaded

        assertEquals(AiCoreDownloadState.Downloading(progressPercent = 62), status.state.first())
    }

    @Test
    fun `model download alone counts missing speech model as zero`() = runTest {
        modelState.value = ModelState.Downloading(
            progress = 1f,
            bytesDownloaded = 3_000,
            bytesTotal = 3_000,
        )
        speechState.value = SpeechModelState.NotDownloaded

        assertEquals(AiCoreDownloadState.Downloading(progressPercent = 75), status.state.first())
    }

    @Test
    fun `engine loading after download is idle`() = runTest {
        modelState.value = ModelState.Loading
        speechState.value = SpeechModelState.Downloaded

        assertEquals(AiCoreDownloadState.Idle, status.state.first())
    }
}
