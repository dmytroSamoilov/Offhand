package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.AiBackend
import com.dmytrosamoilov.offhand.core.ai.api.AiBackendException
import com.dmytrosamoilov.offhand.core.ai.api.AiResult
import com.dmytrosamoilov.offhand.core.ai.api.HardwareBackend
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.ai.api.TranscriptionResult
import com.dmytrosamoilov.offhand.core.audio.AudioChunk
import com.dmytrosamoilov.offhand.core.audio.ChunkBoundaryReason
import com.dmytrosamoilov.offhand.core.audio.VadSnapshot
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.core.data.domain.CalendarEventSuggestion
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.security.AudioOutputStream
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CompleteNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CreateImportedNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CreateRecordingNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.DiscardNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.FailNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAiCoreDownloadedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsCalendarSuggestionsAvailableUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsThinkingEnabledUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MarkNoteProcessingUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MarkNoteRecordedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.RegisterSavedRecordingUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SaveNoteSuggestionsUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SaveNoteTranscriptUseCase
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
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

    private val recorder: AudioRecorder = mockk {
        every { externalInputName } returns MutableStateFlow(null)
    }
    private val audioBackup: RecordingAudioBackup = mockk(relaxed = true) {
        every { openWrite(any()) } returns true
        every { write(any()) } returns true
    }

    private fun stubBackupRead(totalBytes: Int) {
        every { audioBackup.openRead(any()) } returns true
        var delivered = 0
        every { audioBackup.readChunk(any(), any(), any()) } answers {
            val requested = thirdArg<Int>()
            val remaining = totalBytes - delivered
            if (remaining <= 0) -1 else minOf(requested, remaining).also { delivered += it }
        }
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
    private val getNoteStyle: GetNoteStyleUseCase = mockk {
        coEvery { this@mockk.invoke() } returns NoteStyleRef.BuiltIn(NotePreset.SUMMARY)
    }
    private val getNote: GetNoteUseCase = mockk {
        coEvery { this@mockk(any()) } returns null
    }
    private val calendarEventExtractor: CalendarEventExtractor = mockk {
        coEvery { extract(any()) } returns emptyList()
    }
    private val saveNoteSuggestions: SaveNoteSuggestionsUseCase = mockk {
        coEvery { this@mockk(any(), any()) } returns true
    }
    private val isCalendarSuggestionsAvailable: IsCalendarSuggestionsAvailableUseCase = mockk {
        coEvery { this@mockk() } returns true
    }
    private val createImportedNote: CreateImportedNoteUseCase = mockk()
    private val audioDecoder: AudioDecoder = mockk(relaxed = true)
    private val audioStore: EncryptedAudioStore = mockk {
        every { newRecordingFileName() } returns "note-1.pcm.enc"
        every { openForWrite("note-1.pcm.enc") } returns AudioOutputStream(ByteArrayOutputStream())
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
        style = NoteStyleRef.BuiltIn(NotePreset.SUMMARY),
    )

    private fun sttResult(text: String) = TranscriptionResult(
        text = text,
        processingTimeMs = 200,
    )

    private fun stubPolish(json: String) {
        coEvery {
            aiBackend.processText(ModelPromptSet.Gemma4.polishNote(BuiltInNoteStyles.spec(NotePreset.SUMMARY), thinkingEnabled = false), any())
        } returns AiResult(
            text = json,
            processingTimeMs = 0,
            inputTokens = 5,
            outputTokens = 5,
            hardwareBackend = HardwareBackend.CPU,
        )
    }

    private val isThinkingEnabled: IsThinkingEnabledUseCase = mockk {
        every { this@mockk.invoke() } returns false
    }

    private val defaultNoteTitleProvider: DefaultNoteTitleProvider = mockk {
        every { untitledTitle() } returns "Voice note"
    }

    private fun CoroutineScope.manager() = RecordingSessionManager(
        recorder = recorder,
        speechToText = speechToText,
        transcriptStructurer =
            TranscriptStructurer(aiBackend, testModelManager(), isThinkingEnabled, defaultNoteTitleProvider, NoteStyleResolver(mockk())),
        createRecordingNote = createRecordingNote,
        createImportedNote = createImportedNote,
        markNoteRecorded = markNoteRecorded,
        discardNote = discardNote,
        completeNote = completeNote,
        failNote = failNote,
        markNoteProcessing = markNoteProcessing,
        registerSavedRecording = registerSavedRecording,
        saveNoteTranscript = saveNoteTranscript,
        isAiCoreDownloaded = isAiCoreDownloaded,
        getNoteStyle = getNoteStyle,
        getNote = getNote,
        calendarEventExtractor = calendarEventExtractor,
        saveNoteSuggestions = saveNoteSuggestions,
        isCalendarSuggestionsAvailable = isCalendarSuggestionsAvailable,
        audioStore = audioStore,
        audioBackup = audioBackup,
        audioDecoder = audioDecoder,
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
        coEvery { createRecordingNote("note-1.pcm.enc", NoteStyleRef.BuiltIn(NotePreset.SUMMARY)) } returns 42L
        coEvery { markNoteRecorded(42L, any(), "note-1.pcm.enc") } returns storedNote(42L)
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(BuiltInNoteStyles.spec(NotePreset.SUMMARY)), any()) } returns AiResult(
            text = """{"title": "Meeting notes", "overview": "- first\n- second"}""",
            processingTimeMs = 300,
            inputTokens = 20,
            outputTokens = 20,
            hardwareBackend = HardwareBackend.CPU,
        )
        stubPolish("""{"title": "Meeting notes", "overview": "- first\n- second"}""")
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
                body = "- first\n- second",
                transcript = "first part of the meeting\n\nsecond part of the meeting",
                transcriptionTimeMs = 400,
                structuringTimeMs = 300,
                hardwareBackend = "CPU",
                style = NoteStyleRef.BuiltIn(NotePreset.SUMMARY),
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
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(BuiltInNoteStyles.spec(NotePreset.SUMMARY)), any()) } returns AiResult(
            text = """{"title": "Partial notes", "overview": "- good chunk content"}""",
            processingTimeMs = 100,
            inputTokens = 5,
            outputTokens = 5,
            hardwareBackend = HardwareBackend.CPU,
        )
        stubPolish("""{"title": "Partial notes", "overview": "- good chunk content"}""")
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any(), any()) } returns true

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
                structuringTimeMs = 100,
                hardwareBackend = "CPU",
                style = NoteStyleRef.BuiltIn(NotePreset.SUMMARY),
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
        stubBackupRead(64_000)
        coEvery { speechToText.transcribe(any()) } returns sttResult("recovered transcript")
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(BuiltInNoteStyles.spec(NotePreset.SUMMARY)), any()) } returns AiResult(
            text = """{"title": "Recovered", "overview": "- body"}""",
            processingTimeMs = 100,
            inputTokens = 5,
            outputTokens = 5,
            hardwareBackend = HardwareBackend.CPU,
        )
        stubPolish("""{"title": "Recovered", "overview": "- body"}""")
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any(), any()) } returns true

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
                structuringTimeMs = 100,
                hardwareBackend = "CPU",
                style = NoteStyleRef.BuiltIn(NotePreset.SUMMARY),
            )
        }
        verify { speechToText.release() }
    }

    @Test
    fun `imported audio is decoded into the store and processed like a retry`() = runTest {
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        val source = AudioImportSource(handle = "/cache/imports/x", displayName = "Client call.m4a")
        coEvery { createImportedNote("Client call", "note-1.pcm.enc", NoteStyleRef.BuiltIn(NotePreset.SUMMARY)) } returns 9L
        coEvery { audioDecoder.decode(source, any(), any()) } answers {
            secondArg<(ByteArray, Int) -> Unit>().invoke(ByteArray(32_000), 32_000)
            DecodedAudio(durationMs = 1_000)
        }
        coEvery { markNoteRecorded(9L, 1_000L, "note-1.pcm.enc") } returns storedNote(9L)
        every { audioStore.sizeOf("note-1.pcm.enc") } returns 32_000L
        stubBackupRead(32_000)
        coEvery { speechToText.transcribe(any()) } returns sttResult("imported words")
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(BuiltInNoteStyles.spec(NotePreset.SUMMARY)), any()) } returns AiResult(
            text = """{"title": "Client call", "overview": "- imported"}""",
            processingTimeMs = 100,
            inputTokens = 5,
            outputTokens = 5,
            hardwareBackend = HardwareBackend.CPU,
        )
        stubPolish("""{"title": "Client call", "overview": "- imported"}""")
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        val manager = manager()
        manager.importAudio(source)
        testScheduler.advanceUntilIdle()

        assertTrue(manager.processingNoteIds.value.isEmpty())
        coVerify {
            completeNote(
                noteId = 9L,
                title = "Client call",
                body = "- imported",
                transcript = "imported words",
                transcriptionTimeMs = 200,
                structuringTimeMs = 100,
                hardwareBackend = "CPU",
                style = NoteStyleRef.BuiltIn(NotePreset.SUMMARY),
            )
        }
        verify { audioDecoder.discard(source) }
    }

    @Test
    fun `rejected import removes the placeholder note and reports why`() = runTest {
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        val source = AudioImportSource(handle = "/cache/imports/y", displayName = "movie.mkv")
        coEvery { createImportedNote("movie", "note-1.pcm.enc", NoteStyleRef.BuiltIn(NotePreset.SUMMARY)) } returns 4L
        coEvery { audioDecoder.decode(source, any(), any()) } throws AudioImportException.Unsupported()
        justRun { audioStore.delete("note-1.pcm.enc") }
        val events = mutableListOf<NoteProcessingEvent>()

        val manager = manager()
        manager.events.onEach { events += it }.launchIn(this)
        testScheduler.runCurrent()
        manager.importAudio(source)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf<NoteProcessingEvent>(NoteProcessingEvent.ImportRejected(4L, ImportRejection.UNSUPPORTED)), events)
        assertTrue(manager.processingNoteIds.value.isEmpty())
        coVerify { discardNote(4L) }
        verify { audioStore.delete("note-1.pcm.enc") }
        verify { audioDecoder.discard(source) }
        coroutineContext.cancelChildren()
    }

    @Test
    fun `discard deletes the note and its audio`() = runTest {
        val liveChunks = Channel<AudioChunk>()
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        every { recorder.recordStream(pcmSink = any()) } returns liveChunks.consumeAsFlow()
        justRun { recorder.resetVad() }
        justRun { recorder.stop() }
        justRun { audioStore.delete("note-1.pcm.enc") }
        coEvery { createRecordingNote("note-1.pcm.enc", NoteStyleRef.BuiltIn(NotePreset.SUMMARY)) } returns 5L

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
                style = any(),
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
                style = NoteStyleRef.BuiltIn(NotePreset.SUMMARY),
            )
        }
        coroutineContext.cancelChildren()
    }

    @Test
    fun `truncated stored audio keeps the readable part on retry`() = runTest {
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        coEvery { markNoteProcessing(7L) } returns storedNote(7L)
        every { audioStore.sizeOf("note-7.pcm.enc") } returns 200_000L
        stubBackupRead(100_000)
        coEvery { speechToText.transcribe(any()) } returns sttResult("recovered transcript")
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(BuiltInNoteStyles.spec(NotePreset.SUMMARY)), any()) } returns AiResult(
            text = """{"title": "Recovered", "overview": "- body"}""",
            processingTimeMs = 100,
            inputTokens = 5,
            outputTokens = 5,
            hardwareBackend = HardwareBackend.CPU,
        )
        stubPolish("""{"title": "Recovered", "overview": "- body"}""")
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        val manager = manager()
        manager.retryNote(7L, "note-7.pcm.enc")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { speechToText.transcribe(any()) }
        coVerify {
            completeNote(
                noteId = 7L,
                title = "Recovered",
                body = "- body",
                transcript = "recovered transcript",
                transcriptionTimeMs = 200,
                structuringTimeMs = 100,
                hardwareBackend = "CPU",
                style = NoteStyleRef.BuiltIn(NotePreset.SUMMARY),
            )
        }
    }


    @Test
    fun `finished note gets its calendar suggestions extracted and saved`() = runTest {
        val readyNote = storedNote(7L, transcript = "Call Anna on Monday").copy(status = NoteStatus.READY)
        val event = CalendarEventSuggestion("Call Anna", 1_000L, 2_000L, false, "", "")
        coEvery { getNote(7L) } returns readyNote
        coEvery { markNoteProcessing(7L) } returns storedNote(7L)
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        every { audioStore.sizeOf("note-7.pcm.enc") } returns 32_000L
        stubBackupRead(32_000)
        coEvery { speechToText.transcribe(any()) } returns sttResult("Call Anna on Monday")
        coEvery { aiBackend.processText(ModelPromptSet.Gemma4.structureNote(BuiltInNoteStyles.spec(NotePreset.SUMMARY)), any()) } returns AiResult(
            text = """{"title": "Call", "overview": "- Anna"}""",
            processingTimeMs = 300,
            inputTokens = 20,
            outputTokens = 20,
            hardwareBackend = HardwareBackend.CPU,
        )
        stubPolish("""{"title": "Call", "overview": "- Anna"}""")
        coEvery { completeNote(any(), any(), any(), any(), any(), any(), any(), any()) } returns true
        coEvery { calendarEventExtractor.extract(readyNote) } returns listOf(event)

        val manager = manager()
        manager.retryNote(7L, "note-7.pcm.enc")
        testScheduler.advanceUntilIdle()

        coVerify { saveNoteSuggestions(7L, listOf(event)) }
        assertTrue(manager.processingNoteIds.value.isEmpty())
        assertTrue(manager.suggestingNoteIds.value.isEmpty())
    }

    @Test
    fun `on-demand suggestions skip locked and unfinished notes`() = runTest {
        every { recorder.vad } returns MutableStateFlow(VadSnapshot())
        coEvery { getNote(8L) } returns storedNote(8L).copy(status = NoteStatus.READY)
        coEvery { isCalendarSuggestionsAvailable() } returns false
        coEvery { getNote(9L) } returns storedNote(9L)

        val manager = manager()
        manager.suggestEvents(8L)
        manager.suggestEvents(9L)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { calendarEventExtractor.extract(any()) }
        coVerify(exactly = 0) { saveNoteSuggestions(any(), any()) }
        assertTrue(manager.processingNoteIds.value.isEmpty())
    }
}
