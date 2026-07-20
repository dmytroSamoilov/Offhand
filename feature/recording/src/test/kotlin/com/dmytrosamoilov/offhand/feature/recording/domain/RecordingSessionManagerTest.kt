package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.AiResult
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.ai.api.TranscriptionResult
import com.dmytrosamoilov.offhand.core.audio.AudioChunk
import com.dmytrosamoilov.offhand.core.audio.ChunkBoundaryReason
import com.dmytrosamoilov.offhand.core.audio.StreamingAudioRecorder
import com.dmytrosamoilov.offhand.core.audio.VadSnapshot
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CompleteNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CreateProcessingNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.FailNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MarkNoteProcessingUseCase
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingSessionManagerTest {

    private val recorder: StreamingAudioRecorder = mockk {
        every { externalInputName } returns MutableStateFlow(null)
    }
    private val speechToText: SpeechToText = mockk {
        coJustRun { prepare() }
        justRun { release() }
    }
    private val aiBackend: AiBackend = mockk()
    private val createProcessingNote: CreateProcessingNoteUseCase = mockk()
    private val completeNote: CompleteNoteUseCase = mockk()
    private val failNote: FailNoteUseCase = mockk()
    private val markNoteProcessing: MarkNoteProcessingUseCase = mockk()
    private val audioStore: EncryptedAudioStore = mockk {
        every { newRecordingFileName() } returns "note-1.pcm.enc"
        every { openForWrite("note-1.pcm.enc") } returns ByteArrayOutputStream()
    }

    private fun chunk(id: Int) = AudioChunk(
        id = id,
        wav = ByteArray(44 + 320) { id.toByte() },
        durationMs = 10L,
        reason = ChunkBoundaryReason.SILENCE_GAP,
    )

    private fun sttResult(text: String) = TranscriptionResult(
        text = text,
        processingTimeMs = 200,
    )

    private fun stubProofreadEcho() {
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.proofreadTranscript, any()) } answers {
            AiResult(
                text = secondArg(),
                processingTimeMs = 50,
                inputTokens = 5,
                outputTokens = 5,
                hardwareBackend = HardwareBackend.CPU,
            )
        }
    }

    private fun CoroutineScope.manager() = RecordingSessionManager(
        recorder = recorder,
        speechToText = speechToText,
        transcriptProofreader = TranscriptProofreader(aiBackend, testModelManager()),
        transcriptStructurer = TranscriptStructurer(aiBackend, testModelManager()),
        createProcessingNote = createProcessingNote,
        completeNote = completeNote,
        failNote = failNote,
        markNoteProcessing = markNoteProcessing,
        audioStore = audioStore,
        scope = this,
    )

    @Test
    fun `session saves placeholder at drain, completes note in background`() = runTest {
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        every { recorder.recordStream(pcmSink = any()) } returns flowOf(chunk(1), chunk(2))
        justRun { recorder.resetVad() }
        coEvery { speechToText.transcribe(any()) } returnsMany listOf(
            sttResult("first part of the meeting"),
            sttResult("second part of the meeting"),
        )
        coEvery { createProcessingNote("note-1.pcm.enc", any()) } returns 42L
        stubProofreadEcho()
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote, any()) } returns AiResult(
            text = """{"title": "Meeting notes", "overview": "- first\n- second"}""",
            processingTimeMs = 300,
            inputTokens = 20,
            outputTokens = 20,
            hardwareBackend = HardwareBackend.CPU,
        )
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any()) } returns true
        val events = mutableListOf<NoteProcessingEvent>()

        val manager = manager()
        manager.events.onEach { events += it }.launchIn(this)
        testScheduler.runCurrent()
        manager.start()
        testScheduler.advanceUntilIdle()

        assertEquals(SessionPhase.IDLE, manager.session.value.phase)
        assertTrue(manager.processingNoteIds.value.isEmpty())
        assertEquals(listOf<NoteProcessingEvent>(NoteProcessingEvent.Completed(42L)), events)
        verify { speechToText.release() }
        coVerify {
            completeNote(
                noteId = 42L,
                title = "Meeting notes",
                body = "- first\n- second",
                transcript = "first part of the meeting\n\nsecond part of the meeting",
                transcriptionTimeMs = 400,
                structuringTimeMs = 400,
                hardwareBackend = "CPU",
            )
        }
        coroutineContext.cancelChildren()
    }

    @Test
    fun `failed chunk is skipped and remaining transcript still completes the note`() = runTest {
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        every { recorder.recordStream(pcmSink = any()) } returns flowOf(chunk(1), chunk(2))
        justRun { recorder.resetVad() }
        coEvery { speechToText.transcribe(any()) } returns
            sttResult("only good chunk") andThenThrows IllegalStateException("engine hiccup")
        coEvery { createProcessingNote(any(), any()) } returns 7L
        stubProofreadEcho()
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote, any()) } returns AiResult(
            text = """{"title": "Partial notes", "overview": "- good chunk content"}""",
            processingTimeMs = 100,
            inputTokens = 5,
            outputTokens = 5,
            hardwareBackend = HardwareBackend.CPU,
        )
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any()) } returns true

        val manager = manager()
        manager.start()
        testScheduler.advanceUntilIdle()

        assertEquals(SessionPhase.IDLE, manager.session.value.phase)
        coVerify {
            completeNote(
                noteId = 7L,
                title = "Partial notes",
                body = "- good chunk content",
                transcript = "only good chunk",
                transcriptionTimeMs = 200,
                structuringTimeMs = 150,
                hardwareBackend = "CPU",
            )
        }
    }

    @Test
    fun `pause and resume toggle recorder and session state`() = runTest {
        val liveStream = MutableSharedFlow<AudioChunk>()
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        every { recorder.recordStream(pcmSink = any()) } returns liveStream
        justRun { recorder.resetVad() }
        justRun { recorder.pause() }
        justRun { recorder.resume() }

        val manager = manager()
        manager.start()
        testScheduler.runCurrent()
        assertEquals(SessionPhase.RECORDING, manager.session.value.phase)

        manager.pause()
        assertTrue(manager.session.value.isPaused)
        verify { recorder.pause() }

        manager.resume()
        assertFalse(manager.session.value.isPaused)
        verify { recorder.resume() }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `retry re-transcribes stored audio and completes the note`() = runTest {
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        coEvery { markNoteProcessing(7L) } returns true
        every { audioStore.openForRead("note-7.pcm.enc") } returns
            ByteArrayInputStream(ByteArray(64_000))
        coEvery { speechToText.transcribe(any()) } returns sttResult("recovered transcript")
        stubProofreadEcho()
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote, any()) } returns AiResult(
            text = """{"title": "Recovered", "overview": "- body"}""",
            processingTimeMs = 100,
            inputTokens = 5,
            outputTokens = 5,
            hardwareBackend = HardwareBackend.CPU,
        )
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any()) } returns true

        val manager = manager()
        manager.retryNote(7L, "note-7.pcm.enc")
        testScheduler.advanceUntilIdle()

        assertTrue(manager.processingNoteIds.value.isEmpty())
        coVerify {
            completeNote(
                noteId = 7L,
                title = "Recovered",
                body = "- body",
                transcript = "recovered transcript",
                transcriptionTimeMs = 200,
                structuringTimeMs = 150,
                hardwareBackend = "CPU",
            )
        }
        verify { speechToText.release() }
    }

    @Test
    fun `discard deletes audio and skips note creation`() = runTest {
        val liveChunks = Channel<AudioChunk>()
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        every { recorder.recordStream(pcmSink = any()) } returns liveChunks.consumeAsFlow()
        justRun { recorder.resetVad() }
        justRun { recorder.stop() }
        justRun { audioStore.delete("note-1.pcm.enc") }

        val manager = manager()
        manager.start()
        testScheduler.runCurrent()
        assertEquals(SessionPhase.RECORDING, manager.session.value.phase)

        manager.discard()
        liveChunks.close()
        testScheduler.advanceUntilIdle()

        assertEquals(SessionPhase.IDLE, manager.session.value.phase)
        verify { recorder.stop() }
        verify { audioStore.delete("note-1.pcm.enc") }
        coVerify(exactly = 0) { createProcessingNote(any(), any()) }
        coroutineContext.cancelChildren()
    }

    @Test
    fun `silent recording marks the note as failed`() = runTest {
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        every { recorder.recordStream(pcmSink = any()) } returns flowOf(chunk(1))
        justRun { recorder.resetVad() }
        coEvery { speechToText.transcribe(any()) } returns sttResult("   ")
        coEvery { createProcessingNote(any(), any()) } returns 9L
        coEvery { failNote(9L) } returns true
        val events = mutableListOf<NoteProcessingEvent>()

        val manager = manager()
        manager.events.onEach { events += it }.launchIn(this)
        testScheduler.runCurrent()
        manager.start()
        testScheduler.advanceUntilIdle()

        assertEquals(SessionPhase.IDLE, manager.session.value.phase)
        assertEquals(listOf<NoteProcessingEvent>(NoteProcessingEvent.Failed(9L)), events)
        coVerify { failNote(9L) }
        coroutineContext.cancelChildren()
    }
}
