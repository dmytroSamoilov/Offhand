package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.SpeechModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.data.domain.AppForegroundState
import com.dmytrosamoilov.offhand.core.data.domain.NoteFeatures
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetNoteFeaturesUseCase
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpeechWarmUpTest {

    private val isInForeground = MutableStateFlow(true)
    private val session = MutableStateFlow(RecordingSession())
    private val processingNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val transcribingNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val downloadState = MutableStateFlow<SpeechModelState>(SpeechModelState.Downloaded)

    private val speechToText: SpeechToText = mockk {
        every { downloadState } returns this@SpeechWarmUpTest.downloadState
        coJustRun { prepare() }
        justRun { release() }
    }
    private val sessionManager: RecordingSessionManager = mockk {
        every { session } returns this@SpeechWarmUpTest.session
        every { processingNoteIds } returns this@SpeechWarmUpTest.processingNoteIds
        every { transcribingNoteIds } returns this@SpeechWarmUpTest.transcribingNoteIds
    }
    private val appForegroundState: AppForegroundState = mockk {
        every { isInForeground } returns this@SpeechWarmUpTest.isInForeground
    }
    private val getNoteFeatures: GetNoteFeaturesUseCase = mockk {
        coEvery { this@mockk.invoke() } returns NoteFeatures()
    }

    private fun TestScope.started() {
        SpeechWarmUp(speechToText, sessionManager, appForegroundState, getNoteFeatures).start(backgroundScope)
        testScheduler.runCurrent()
    }

    @Test
    fun `the engine is loaded while the app is in front and freed once it is away`() = runTest {
        started()
        coVerify(exactly = 1) { speechToText.prepare() }

        isInForeground.value = false
        testScheduler.runCurrent()

        verify(exactly = 1) { speechToText.release() }
    }

    @Test
    fun `a recording that goes on in the background keeps its engine until it is finished`() = runTest {
        started()
        session.value = RecordingSession(phase = SessionPhase.RECORDING)
        isInForeground.value = false
        testScheduler.runCurrent()
        verify(exactly = 0) { speechToText.release() }

        session.value = RecordingSession(phase = SessionPhase.IDLE)
        processingNoteIds.value = setOf(7L)
        testScheduler.runCurrent()
        verify(exactly = 0) { speechToText.release() }

        processingNoteIds.value = emptySet()
        testScheduler.runCurrent()
        verify(exactly = 1) { speechToText.release() }
    }

    @Test
    fun `nothing is loaded without the model on disk or with transcription switched off`() = runTest {
        downloadState.value = SpeechModelState.NotDownloaded
        started()
        coVerify(exactly = 0) { speechToText.prepare() }

        coEvery { getNoteFeatures() } returns NoteFeatures(isTranscriptionEnabled = false)
        downloadState.value = SpeechModelState.Downloaded
        testScheduler.runCurrent()
        coVerify(exactly = 0) { speechToText.prepare() }
    }

    @Test
    fun `the engine is warmed again once the model has arrived and after a note is finished`() = runTest {
        downloadState.value = SpeechModelState.NotDownloaded
        started()
        downloadState.value = SpeechModelState.Downloaded
        testScheduler.runCurrent()
        coVerify(exactly = 1) { speechToText.prepare() }

        processingNoteIds.value = setOf(7L)
        testScheduler.runCurrent()
        processingNoteIds.value = emptySet()
        testScheduler.runCurrent()

        coVerify(exactly = 2) { speechToText.prepare() }
    }
}
