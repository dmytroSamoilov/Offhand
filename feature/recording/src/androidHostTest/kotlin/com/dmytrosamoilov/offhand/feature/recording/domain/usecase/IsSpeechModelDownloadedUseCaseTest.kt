package com.dmytrosamoilov.offhand.feature.recording.domain.usecase

import com.dmytrosamoilov.offhand.core.ai.api.SpeechModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsSpeechModelDownloadedUseCaseTest {

    private val speechModelState = MutableStateFlow<SpeechModelState>(SpeechModelState.NotDownloaded)
    private val speechToText: SpeechToText = mockk {
        every { downloadState } returns speechModelState
    }

    private val useCase = IsSpeechModelDownloadedUseCase(speechToText)

    @Test
    fun `speech files on disk count as downloaded`() {
        speechModelState.value = SpeechModelState.Downloaded

        assertTrue(useCase())
    }

    @Test
    fun `missing speech files do not count as downloaded`() {
        assertFalse(useCase())
    }
}
