package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.AiBackendException
import com.dmytrosamoilov.offhand.core.ai.api.AiResult
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.ai.api.TranscriptionResult
import com.dmytrosamoilov.offhand.core.audio.AudioChunk
import com.dmytrosamoilov.offhand.core.audio.ChunkBoundaryReason
import com.dmytrosamoilov.offhand.core.audio.StreamingAudioRecorder
import com.dmytrosamoilov.offhand.core.audio.VadSnapshot
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CompleteNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CreateRecordingNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.DiscardNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.FailNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetNotePresetUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAiCoreDownloadedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MarkNoteProcessingUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MarkNoteRecordedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.RegisterSavedRecordingUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SaveNoteTranscriptUseCase
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
    private val createRecordingNote: CreateRecordingNoteUseCase = mockk()
    private val markNoteRecorded: MarkNoteRecordedUseCase = mockk()
    private val discardNote: DiscardNoteUseCase = mockk {
        coJustRun { this@mockk.invoke(any()) }
    }
    private val completeNote: CompleteNoteUseCase = mockk()
    private val failNote: FailNoteUseCase = mockk()
    private val markNoteProcessing: MarkNoteProcessingUseCase = mockk()
    private val registerSavedRecording: RegisterSavedRecordingUseCase = mockk {
        coJustRun { this@mockk.invoke() }
    }
    private val saveNoteTranscript: SaveNoteTranscriptUseCase = mockk {
        coJustRun { this@mockk.invoke(any(), any(), any()) }
    }
    private val isAiCoreDownloaded: IsAiCoreDownloadedUseCase = mockk {
        coEvery { this@mockk.invoke() } returns true
    }
    private val getNotePreset: GetNotePresetUseCase = mockk {
        coEvery { this@mockk.invoke() } returns NotePreset.SUMMARY
    }
    private val getNote: GetNoteUseCase = mockk()
    private val audioStore: EncryptedAudioStore = mockk {
        every { newRecordingFileName() } returns "note-1.pcm.enc"
        every { openForWrite("note-1.pcm.enc") } returns ByteArrayOutputStream()
    }

    private fun chunk(id: Int, speechMs: Long = 2_000L) = AudioChunk(
        id = id,
        wav = ByteArray(44 + 320) { id.toByte() },
        durationMs = 10L,
        speechMs = speechMs,
        reason = ChunkBoundaryReason.SILENCE_GAP,
    )

    private fun storedNote(id: Long, transcript: String = "recovered transcript") = Note(
        id = id,
        title = "Recording",
        body = "",
        transcript = transcript,
        createdAtEpochMs = 0,
        transcriptionTimeMs = null,
        structuringTimeMs = null,
        hardwareBackend = null,
        status = NoteStatus.PROCESSING,
        preset = NotePreset.SUMMARY,
    )

    private fun sttResult(text: String) = TranscriptionResult(
        text = text,
        processingTimeMs = 200,
    )

    private fun CoroutineScope.manager() = RecordingSessionManager(
        recorder = recorder,
        speechToText = speechToText,
        transcriptStructurer = TranscriptStructurer(aiBackend, testModelManager()),
        createRecordingNote = createRecordingNote,
        markNoteRecorded = markNoteRecorded,
        discardNote = discardNote,
        completeNote = completeNote,
        failNote = failNote,
        markNoteProcessing = markNoteProcessing,
        registerSavedRecording = registerSavedRecording,
        saveNoteTranscript = saveNoteTranscript,
        isAiCoreDownloaded = isAiCoreDownloaded,
        getNotePreset = getNotePreset,
        getNote = getNote,
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
        coEvery { createRecordingNote("note-1.pcm.enc", NotePreset.SUMMARY) } returns 42L
        coEvery { markNoteRecorded(42L, any(), "note-1.pcm.enc") } returns storedNote(42L)
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(NotePreset.SUMMARY), any()) } returns AiResult(
            text = """{"title": "Meeting notes", "overview": "- first\n- second"}""",
            processingTimeMs = 300,
            inputTokens = 20,
            outputTokens = 20,
            hardwareBackend = HardwareBackend.CPU,
        )
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any(), any()) } returns true
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
        coVerify(exactly = 1) { registerSavedRecording.invoke() }
        coVerify {
            saveNoteTranscript(
                noteId = 42L,
                transcript = "first part of the meeting\n\nsecond part of the meeting",
                transcriptionTimeMs = 400,
            )
        }
        coVerify {
            completeNote(
                noteId = 42L,
                title = "Meeting notes",
                body = "first\nsecond",
                transcript = "first part of the meeting\n\nsecond part of the meeting",
                transcriptionTimeMs = 400,
                structuringTimeMs = 300,
                hardwareBackend = "CPU",
                preset = NotePreset.SUMMARY,
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
        coEvery { createRecordingNote(any(), any()) } returns 7L
        coEvery { markNoteRecorded(7L, any(), any()) } returns storedNote(7L)
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(NotePreset.SUMMARY), any()) } returns AiResult(
            text = """{"title": "Partial notes", "overview": "- good chunk content"}""",
            processingTimeMs = 100,
            inputTokens = 5,
            outputTokens = 5,
            hardwareBackend = HardwareBackend.CPU,
        )
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        val manager = manager()
        manager.start()
        testScheduler.advanceUntilIdle()

        assertEquals(SessionPhase.IDLE, manager.session.value.phase)
        coVerify {
            completeNote(
                noteId = 7L,
                title = "Partial notes",
                body = "good chunk content",
                transcript = "only good chunk",
                transcriptionTimeMs = 200,
                structuringTimeMs = 100,
                hardwareBackend = "CPU",
                preset = NotePreset.SUMMARY,
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
        coEvery { markNoteProcessing(7L) } returns storedNote(7L)
        every { audioStore.sizeOf("note-7.pcm.enc") } returns 64_000L
        every { audioStore.openForRead("note-7.pcm.enc") } returns
            ByteArrayInputStream(ByteArray(64_000))
        coEvery { speechToText.transcribe(any()) } returns sttResult("recovered transcript")
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(NotePreset.SUMMARY), any()) } returns AiResult(
            text = """{"title": "Recovered", "overview": "- body"}""",
            processingTimeMs = 100,
            inputTokens = 5,
            outputTokens = 5,
            hardwareBackend = HardwareBackend.CPU,
        )
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        val manager = manager()
        manager.retryNote(7L, "note-7.pcm.enc")
        testScheduler.advanceUntilIdle()

        assertTrue(manager.processingNoteIds.value.isEmpty())
        coVerify {
            completeNote(
                noteId = 7L,
                title = "Recovered",
                body = "body",
                transcript = "recovered transcript",
                transcriptionTimeMs = 200,
                structuringTimeMs = 100,
                hardwareBackend = "CPU",
                preset = NotePreset.SUMMARY,
            )
        }
        verify { speechToText.release() }
    }

    @Test
    fun `discard deletes the note and its audio`() = runTest {
        val liveChunks = Channel<AudioChunk>()
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        every { recorder.recordStream(pcmSink = any()) } returns liveChunks.consumeAsFlow()
        justRun { recorder.resetVad() }
        justRun { recorder.stop() }
        justRun { audioStore.delete("note-1.pcm.enc") }
        coEvery { createRecordingNote("note-1.pcm.enc", NotePreset.SUMMARY) } returns 5L

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
        coVerify { discardNote(5L) }
        coVerify(exactly = 0) { markNoteRecorded(any(), any(), any()) }
        coroutineContext.cancelChildren()
    }

    @Test
    fun `silent recording marks the note as failed`() = runTest {
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        every { recorder.recordStream(pcmSink = any()) } returns flowOf(chunk(1))
        justRun { recorder.resetVad() }
        coEvery { speechToText.transcribe(any()) } returns sttResult("   ")
        coEvery { createRecordingNote(any(), any()) } returns 9L
        coEvery { markNoteRecorded(9L, any(), any()) } returns storedNote(9L)
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

    @Test
    fun `chunk without speech never reaches the transcriber`() = runTest {
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        every { recorder.recordStream(pcmSink = any()) } returns
            flowOf(chunk(1), chunk(2, speechMs = 0L))
        justRun { recorder.resetVad() }
        coEvery { speechToText.transcribe(any()) } returns sttResult("spoken content")
        coEvery { createRecordingNote(any(), any()) } returns 11L
        coEvery { markNoteRecorded(11L, any(), any()) } returns storedNote(11L)
        coEvery { aiBackend.processText(any(), any()) } returns AiResult(
            text = """{"title": "Spoken", "overview": "- content"}""",
            processingTimeMs = 100,
            inputTokens = 5,
            outputTokens = 5,
            hardwareBackend = HardwareBackend.CPU,
        )
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        val manager = manager()
        manager.start()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { speechToText.transcribe(any()) }
        coVerify {
            completeNote(
                noteId = 11L,
                title = any(),
                body = any(),
                transcript = "spoken content",
                transcriptionTimeMs = any(),
                structuringTimeMs = any(),
                hardwareBackend = any(),
                preset = any(),
            )
        }
    }

    @Test
    fun `structuring failure completes the note with the transcript only`() = runTest {
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        every { recorder.recordStream(pcmSink = any()) } returns flowOf(chunk(1))
        justRun { recorder.resetVad() }
        coEvery { speechToText.transcribe(any()) } returns
            sttResult("budget approved for next quarter")
        coEvery { createRecordingNote(any(), any()) } returns 13L
        coEvery { markNoteRecorded(13L, any(), any()) } returns storedNote(13L)
        coEvery { aiBackend.processText(any(), any()) } throws
            AiBackendException("engine could not load")
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any(), any()) } returns true
        val events = mutableListOf<NoteProcessingEvent>()

        val manager = manager()
        manager.events.onEach { events += it }.launchIn(this)
        testScheduler.runCurrent()
        manager.start()
        testScheduler.advanceUntilIdle()

        assertEquals(listOf<NoteProcessingEvent>(NoteProcessingEvent.Completed(13L)), events)
        coVerify(exactly = 0) { failNote(any()) }
        coVerify {
            completeNote(
                noteId = 13L,
                title = "budget approved for next quarter",
                body = "budget approved for next quarter",
                transcript = "budget approved for next quarter",
                transcriptionTimeMs = 200,
                structuringTimeMs = 0,
                hardwareBackend = "CPU",
                preset = NotePreset.SUMMARY,
            )
        }
        coroutineContext.cancelChildren()
    }

    @Test
    fun `truncated stored audio keeps the readable part on retry`() = runTest {
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        coEvery { markNoteProcessing(7L) } returns storedNote(7L)
        every { audioStore.sizeOf("note-7.pcm.enc") } returns 200_000L
        every { audioStore.openForRead("note-7.pcm.enc") } returns TruncatedStream(100_000)
        coEvery { speechToText.transcribe(any()) } returns sttResult("recovered transcript")
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(NotePreset.SUMMARY), any()) } returns AiResult(
            text = """{"title": "Recovered", "overview": "- body"}""",
            processingTimeMs = 100,
            inputTokens = 5,
            outputTokens = 5,
            hardwareBackend = HardwareBackend.CPU,
        )
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        val manager = manager()
        manager.retryNote(7L, "note-7.pcm.enc")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { speechToText.transcribe(any()) }
        coVerify {
            completeNote(
                noteId = 7L,
                title = "Recovered",
                body = "body",
                transcript = "recovered transcript",
                transcriptionTimeMs = 200,
                structuringTimeMs = 100,
                hardwareBackend = "CPU",
                preset = NotePreset.SUMMARY,
            )
        }
    }

    private class TruncatedStream(private val readableBytes: Int) : java.io.InputStream() {
        private var position = 0

        override fun read(): Int = throw java.io.IOException("ciphertext truncated")

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= readableBytes) throw java.io.IOException("ciphertext truncated")
            val count = minOf(length, readableBytes - position)
            position += count
            return count
        }
    }
}
