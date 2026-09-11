package com.dmytrosamoilov.offhand.feature.recording.domain

import co.touchlab.kermit.Logger
import com.dmytrosamoilov.offhand.core.ai.api.AiBackendException
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.ai.api.TranscriptionResult
import com.dmytrosamoilov.offhand.core.audio.AudioChunk
import com.dmytrosamoilov.offhand.core.audio.VadSnapshot
import com.dmytrosamoilov.offhand.core.audio.WavCodec
import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NoteStyleRef
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.core.security.closeQuietly
import com.dmytrosamoilov.offhand.core.security.writeChunk
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CompleteNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CreateImportedNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CreateRecordingNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.DiscardNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.FailNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetNoteStyleUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.GetNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsAiCoreDownloadedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.IsCalendarSuggestionsAvailableUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MarkNoteProcessingUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MarkNoteRecordedUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.RegisterSavedRecordingUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SaveNoteSuggestionsUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.SaveNoteTranscriptUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecordingSessionManager(
    private val recorder: AudioRecorder,
    private val speechToText: SpeechToText,
    private val transcriptStructurer: TranscriptStructurer,
    private val createRecordingNote: CreateRecordingNoteUseCase,
    private val createImportedNote: CreateImportedNoteUseCase,
    private val markNoteRecorded: MarkNoteRecordedUseCase,
    private val discardNote: DiscardNoteUseCase,
    private val completeNote: CompleteNoteUseCase,
    private val failNote: FailNoteUseCase,
    private val markNoteProcessing: MarkNoteProcessingUseCase,
    private val registerSavedRecording: RegisterSavedRecordingUseCase,
    private val saveNoteTranscript: SaveNoteTranscriptUseCase,
    private val isAiCoreDownloaded: IsAiCoreDownloadedUseCase,
    private val getNoteStyle: GetNoteStyleUseCase,
    private val getNote: GetNoteUseCase,
    private val calendarEventExtractor: CalendarEventExtractor,
    private val saveNoteSuggestions: SaveNoteSuggestionsUseCase,
    private val isCalendarSuggestionsAvailable: IsCalendarSuggestionsAvailableUseCase,
    private val audioStore: EncryptedAudioStore,
    private val audioBackup: RecordingAudioBackup,
    private val audioDecoder: AudioDecoder,
    private val scope: CoroutineScope,
) {

    // Fair mutex: queued notes are processed one at a time, in arrival order.
    private val processingMutex = Mutex()

    private val mutableSession = MutableStateFlow(RecordingSession())
    val session: StateFlow<RecordingSession> = mutableSession.asStateFlow()

    private val mutableProcessingNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    val processingNoteIds: StateFlow<Set<Long>> = mutableProcessingNoteIds.asStateFlow()

    private val mutableSuggestingNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    val suggestingNoteIds: StateFlow<Set<Long>> = mutableSuggestingNoteIds.asStateFlow()

    private val mutableNoteProgress = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val noteProgress: StateFlow<Map<Long, Int>> = mutableNoteProgress.asStateFlow()

    private val mutableEvents = MutableSharedFlow<NoteProcessingEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<NoteProcessingEvent> = mutableEvents.asSharedFlow()

    private val mutableActiveRecordingNoteId = MutableStateFlow<Long?>(null)
    val activeRecordingNoteId: StateFlow<Long?> = mutableActiveRecordingNoteId.asStateFlow()

    val vad: StateFlow<VadSnapshot> = recorder.vad

    val externalMicName: StateFlow<String?> = recorder.externalInputName

    private val transcripts = mutableMapOf<Int, String>()
    private var transcriptionTimeMs = 0L

    private var audioFileName: String? = null

    private var isDiscardRequested = false

    private var sessionStyle: NoteStyleRef = NoteStyleRef.DEFAULT

    fun start() {
        if (mutableSession.value.phase.isActive()) return
        transcripts.clear()
        transcriptionTimeMs = 0
        isDiscardRequested = false
        recorder.resetVad()
        mutableSession.value = RecordingSession(phase = SessionPhase.RECORDING)
        scope.launch { runSession() }
    }

    fun pause() {
        if (mutableSession.value.phase != SessionPhase.RECORDING) return
        recorder.pause()
        mutableSession.update { it.copy(isPaused = true) }
    }

    fun resume() {
        if (mutableSession.value.phase != SessionPhase.RECORDING) return
        recorder.resume()
        mutableSession.update { it.copy(isPaused = false) }
    }

    fun stop() {
        if (mutableSession.value.phase != SessionPhase.RECORDING) return
        mutableSession.update { it.copy(phase = SessionPhase.DRAINING, isPaused = false) }
        recorder.stop()
    }

    fun discard() {
        if (mutableSession.value.phase != SessionPhase.RECORDING) return
        isDiscardRequested = true
        mutableSession.update { it.copy(phase = SessionPhase.DRAINING, isPaused = false) }
        recorder.stop()
    }

    // The saved note id survives the drain so a sheet that missed the
    // conflated update still learns the note was saved; opening or closing
    // the sheet clears it.
    fun resetToIdle() {
        val phase = mutableSession.value.phase
        if (phase == SessionPhase.FAILED || phase == SessionPhase.IDLE) {
            mutableSession.value = RecordingSession()
        }
    }

    fun retryNote(noteId: Long, audioFileName: String) {
        if (noteId in mutableProcessingNoteIds.value) return
        mutableProcessingNoteIds.update { it + noteId }
        scope.launch {
            processingMutex.withLock {
                val note = markNoteProcessing(noteId)
                if (note == null) {
                    mutableProcessingNoteIds.update { it - noteId }
                    return@withLock
                }
                updateProgress(noteId, 0f)
                val stored = transcribeStoredAudio(audioFileName) { fraction ->
                    updateProgress(noteId, fraction * RETRY_WHISPER_SHARE)
                }
                processNote(
                    noteId = noteId,
                    transcripts = stored.texts,
                    transcriptionMs = stored.transcriptionTimeMs,
                    progressOffset = RETRY_WHISPER_SHARE,
                    style = note.style,
                )
            }
        }
    }

    fun restructureNote(noteId: Long, style: NoteStyleRef) {
        if (noteId in mutableProcessingNoteIds.value) return
        mutableProcessingNoteIds.update { it + noteId }
        scope.launch {
            processingMutex.withLock {
                val note = getNote(noteId)?.takeIf { it.transcript.isNotBlank() }
                if (note == null || markNoteProcessing(noteId) == null) {
                    mutableProcessingNoteIds.update { it - noteId }
                    return@withLock
                }
                updateProgress(noteId, 0f)
                processNote(
                    noteId = noteId,
                    transcripts = transcriptStructurer.splitStoredTranscript(note.transcript),
                    transcriptionMs = note.transcriptionTimeMs ?: 0,
                    progressOffset = 0f,
                    style = style,
                )
            }
        }
    }

    fun importAudio(source: AudioImportSource) {
        scope.launch {
            processingMutex.withLock { importLocked(source) }
        }
    }

    private suspend fun importLocked(source: AudioImportSource) {
        val fileName = audioStore.newRecordingFileName()
        val style = getNoteStyle()
        val noteId = createImportedNote(importedTitle(source.displayName), fileName, style)
        mutableProcessingNoteIds.update { it + noteId }
        updateProgress(noteId, 0f)
        val decoded = decodeImport(source, fileName) { fraction ->
            updateProgress(noteId, fraction * IMPORT_DECODE_SHARE)
        }
        if (decoded == null) {
            rejectImport(noteId, fileName, source)
            return
        }
        markNoteRecorded(noteId, decoded.durationMs, fileName)
        val stored = transcribeStoredAudio(fileName) { fraction ->
            updateProgress(noteId, IMPORT_DECODE_SHARE + fraction * (RETRY_WHISPER_SHARE - IMPORT_DECODE_SHARE))
        }
        processNote(noteId, stored.texts, stored.transcriptionTimeMs, RETRY_WHISPER_SHARE, style)
    }

    fun suggestEvents(noteId: Long) {
        if (noteId in mutableProcessingNoteIds.value) return
        mutableProcessingNoteIds.update { it + noteId }
        scope.launch {
            processingMutex.withLock {
                try {
                    suggestEventsLocked(noteId)
                } finally {
                    mutableProcessingNoteIds.update { it - noteId }
                }
            }
        }
    }

    // Still under the processing lock and still counted as processing, so the
    // foreground service outlives the extraction and no retry can interleave.
    private suspend fun suggestEventsLocked(noteId: Long) {
        mutableSuggestingNoteIds.update { it + noteId }
        try {
            val note = getNote(noteId)?.takeIf { it.status == NoteStatus.READY } ?: return
            if (!isCalendarSuggestionsAvailable() || !isAiCoreDownloaded()) return
            saveNoteSuggestions(noteId, calendarEventExtractor.extract(note))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Logger.withTag(LOG_TAG).w(t) { "Calendar suggestions failed for note $noteId" }
        } finally {
            mutableSuggestingNoteIds.update { it - noteId }
        }
    }

    private var lastImportRejection: ImportRejection = ImportRejection.UNREADABLE

    private suspend fun decodeImport(
        source: AudioImportSource,
        fileName: String,
        onProgress: (Float) -> Unit,
    ): DecodedAudio? {
        val stream = audioStore.openForWrite(fileName)
        return try {
            audioDecoder.decode(source, onPcm = { bytes, length -> stream.writeChunk(bytes, length) }, onProgress = onProgress)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (rejected: AudioImportException) {
            Logger.withTag(LOG_TAG).w { "Import rejected: ${rejected.message}" }
            lastImportRejection = rejected.toRejection()
            null
        } catch (t: Throwable) {
            Logger.withTag(LOG_TAG).e(t) { "Import decoding failed" }
            lastImportRejection = ImportRejection.UNREADABLE
            null
        } finally {
            stream.closeQuietly()
            audioDecoder.discard(source)
        }
    }

    private suspend fun rejectImport(noteId: Long, fileName: String, source: AudioImportSource) {
        runCatching { discardNote(noteId) }
        runCatching { audioStore.delete(fileName) }
        mutableProcessingNoteIds.update { it - noteId }
        mutableNoteProgress.update { it - noteId }
        mutableEvents.emit(NoteProcessingEvent.ImportRejected(noteId, lastImportRejection))
    }

    private fun AudioImportException.toRejection(): ImportRejection = when (this) {
        is AudioImportException.Unsupported -> ImportRejection.UNSUPPORTED
        is AudioImportException.TooLong -> ImportRejection.TOO_LONG
        is AudioImportException.Unreadable -> ImportRejection.UNREADABLE
    }

    private fun importedTitle(displayName: String): String =
        displayName.substringBeforeLast('.').trim().ifBlank { displayName }

    internal suspend fun <T> withProcessingLock(block: suspend () -> T): T = processingMutex.withLock { block() }

    private suspend fun transcribeStoredAudio(
        audioFileName: String,
        onProgress: (Float) -> Unit,
    ): StoredTranscription = try {
        speechToText.prepare()
        val approxTotalBytes = audioStore.sizeOf(audioFileName).coerceAtLeast(1L)
        if (audioBackup.openRead(audioFileName)) {
            try {
                transcribePcmWindows(approxTotalBytes, onProgress)
            } finally {
                audioBackup.closeRead()
            }
        } else {
            StoredTranscription(texts = emptyList(), transcriptionTimeMs = 0)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (t: Throwable) {
        Logger.withTag(LOG_TAG).e(t) { "Stored audio transcription failed" }
        StoredTranscription(texts = emptyList(), transcriptionTimeMs = 0)
    } finally {
        speechToText.release()
    }

    private suspend fun transcribePcmWindows(
        approxTotalBytes: Long,
        onProgress: (Float) -> Unit,
    ): StoredTranscription {
        val window = ByteArray((RETRY_CHUNK_MS * BYTES_PER_MS).toInt())
        val texts = mutableListOf<String>()
        var totalTimeMs = 0L
        var bytesRead = 0L
        while (true) {
            val read = readWindow(window)
            if (read <= 0) break
            bytesRead += read
            val wav = WavCodec.wrap(
                pcm = window.copyOfRange(0, read),
                sampleRate = AudioRecorder.SAMPLE_RATE,
                channels = 1,
                bitsPerSample = 16,
            )
            transcribeWindow(wav)?.let { result ->
                totalTimeMs += result.processingTimeMs
                result.text.trim().takeIf { it.isNotBlank() }?.let(texts::add)
            }
            onProgress((bytesRead.toFloat() / approxTotalBytes).coerceAtMost(1f))
            if (read < window.size) break
        }
        onProgress(1f)
        return StoredTranscription(texts = texts, transcriptionTimeMs = totalTimeMs)
    }

    private suspend fun transcribeWindow(wav: ByteArray): TranscriptionResult? = try {
        speechToText.transcribe(wav)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (t: Throwable) {
        Logger.withTag(LOG_TAG).w(t) { "Stored audio window failed, skipping it" }
        null
    }

    // A recording cut off by process death is missing its final ciphertext
    // segment — keep every window that still decrypts instead of dropping
    // the whole file.
    private fun readWindow(window: ByteArray): Int {
        var offset = 0
        while (offset < window.size) {
            val read = audioBackup.readChunk(window, offset, window.size - offset)
            if (read < 0) break
            offset += read
        }
        return offset
    }

    private fun updateProgress(noteId: Long, fraction: Float) {
        val percent = (fraction * 100).toInt().coerceIn(0, 100)
        mutableNoteProgress.update { it + (noteId to percent) }
    }

    private data class StoredTranscription(
        val texts: List<String>,
        val transcriptionTimeMs: Long,
    )

    private suspend fun runSession() {
        sessionStyle = getNoteStyle()
        openAudioBackup()
        mutableActiveRecordingNoteId.value = createSessionNote()
        val queue = Channel<AudioChunk>(Channel.UNLIMITED)
        scope.launch { prepareTranscriber() }
        scope.launch { produceChunks(queue) }
        for (chunk in queue) {
            if (!isDiscardRequested) {
                transcribeChunk(chunk)
            }
        }
        speechToText.release()
        when {
            isDiscardRequested -> finishDiscardedSession()
            mutableSession.value.phase == SessionPhase.FAILED -> salvageFailedSession()
            else -> finishRecordedSession()
        }
    }

    private suspend fun createSessionNote(): Long? = try {
        createRecordingNote(audioFileName, sessionStyle)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (t: Throwable) {
        Logger.withTag(LOG_TAG).e(t) { "Could not create the recording note" }
        null
    }

    private suspend fun finishDiscardedSession() {
        mutableActiveRecordingNoteId.value?.let { noteId ->
            runCatching { discardNote(noteId) }
                .onFailure { Logger.withTag(LOG_TAG).w(it) { "Discarded note cleanup failed" } }
        }
        mutableActiveRecordingNoteId.value = null
        transcripts.clear()
        isDiscardRequested = false
        mutableSession.value = RecordingSession()
    }

    private suspend fun salvageFailedSession() {
        val noteId = mutableActiveRecordingNoteId.value ?: return
        mutableActiveRecordingNoteId.value = null
        val chunkTranscripts = sortedTranscripts()
        when {
            chunkTranscripts.isNotEmpty() -> {
                markNoteRecorded(noteId, recorder.vad.value.totalElapsedMs, audioFileName) ?: return
                startProcessing(noteId, chunkTranscripts, transcriptionTimeMs, sessionStyle)
            }
            audioFileName != null -> {
                markNoteRecorded(noteId, recorder.vad.value.totalElapsedMs, audioFileName)
                failNote(noteId)
            }
            else -> runCatching { discardNote(noteId) }
                .onFailure { Logger.withTag(LOG_TAG).w(it) { "Empty failed note cleanup failed" } }
        }
    }

    private suspend fun prepareTranscriber() {
        try {
            speechToText.prepare()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Logger.withTag(LOG_TAG).w(t) { "Transcriber warm-up failed, retrying per chunk" }
        }
    }

    private suspend fun produceChunks(queue: Channel<AudioChunk>) {
        try {
            recorder.recordStream(pcmSink = ::writeAudioFrame).collect { chunk ->
                addChunk(chunk)
                queue.send(chunk)
            }
            closeAudioBackup()
            if (isDiscardRequested) {
                deleteAudioBackup()
            } else {
                mutableSession.update { it.copy(noteId = mutableActiveRecordingNoteId.value) }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Logger.withTag(LOG_TAG).e(t) { "Recording failed" }
            closeAudioBackup()
            fail(t.message ?: "Recording failed")
        } finally {
            queue.close()
        }
    }

    private fun deleteAudioBackup() {
        audioFileName?.let { fileName -> runCatching { audioStore.delete(fileName) } }
        audioFileName = null
    }

    private fun openAudioBackup() {
        val fileName = audioStore.newRecordingFileName()
        audioFileName = if (audioBackup.openWrite(fileName)) fileName else null
    }

    private fun writeAudioFrame(pcm: ByteArray) {
        val fileName = audioFileName ?: return
        if (!audioBackup.write(pcm)) {
            runCatching { audioStore.delete(fileName) }
            audioFileName = null
        }
    }

    private fun closeAudioBackup() {
        audioBackup.closeWrite()
    }

    private suspend fun transcribeChunk(chunk: AudioChunk) {
        if (chunk.speechMs < MIN_CHUNK_SPEECH_MS) {
            updateChunk(chunk.id) { it.copy(state = ChunkState.DONE) }
            return
        }
        updateChunk(chunk.id) { it.copy(state = ChunkState.TRANSCRIBING) }
        try {
            val result = speechToText.transcribe(chunk.wav)
            transcripts[chunk.id] = result.text.trim()
            transcriptionTimeMs += result.processingTimeMs
            mutableSession.update { it.copy(transcriptionTimeMs = transcriptionTimeMs) }
            updateChunk(chunk.id) { it.copy(state = ChunkState.DONE) }
            persistSessionTranscript()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Logger.withTag(LOG_TAG).w(t) { "Chunk ${chunk.id} transcription failed" }
            updateChunk(chunk.id) { it.copy(state = ChunkState.FAILED) }
        }
    }

    private suspend fun persistSessionTranscript() {
        val noteId = mutableActiveRecordingNoteId.value ?: return
        persistTranscript(noteId, sortedTranscripts(), transcriptionTimeMs)
    }

    private fun sortedTranscripts(): List<String> =
        transcripts.entries.sortedBy { it.key }.map { it.value }.filter { it.isNotBlank() }

    private suspend fun finishRecordedSession() {
        val noteId = mutableSession.value.noteId
        val recorded = noteId?.let {
            markNoteRecorded(it, recorder.vad.value.totalElapsedMs, audioFileName)
        }
        if (noteId == null || recorded == null) {
            fail("Could not save the recording")
            return
        }
        mutableActiveRecordingNoteId.value = null
        registerSavedRecording()
        val chunkTranscripts = sortedTranscripts()
        val recordedTranscriptionMs = transcriptionTimeMs
        val style = sessionStyle
        mutableSession.value = RecordingSession(noteId = noteId)
        startProcessing(noteId, chunkTranscripts, recordedTranscriptionMs, style)
    }

    private fun startProcessing(
        noteId: Long,
        chunkTranscripts: List<String>,
        transcriptionMs: Long,
        style: NoteStyleRef,
    ) {
        mutableProcessingNoteIds.update { it + noteId }
        scope.launch {
            if (!isAiCoreDownloaded()) {
                Logger.withTag(LOG_TAG).w { "AI core not downloaded yet, note $noteId stays queued" }
                mutableProcessingNoteIds.update { it - noteId }
                return@launch
            }
            processingMutex.withLock {
                processNote(noteId, chunkTranscripts, transcriptionMs, 0f, style)
            }
        }
    }

    private suspend fun persistTranscript(
        noteId: Long,
        transcripts: List<String>,
        transcriptionMs: Long,
    ) {
        if (transcripts.isEmpty()) return
        saveNoteTranscript(
            noteId = noteId,
            transcript = transcriptStructurer.joinChunks(transcripts),
            transcriptionTimeMs = transcriptionMs,
        )
    }

    private suspend fun processNote(
        noteId: Long,
        transcripts: List<String>,
        transcriptionMs: Long,
        progressOffset: Float,
        style: NoteStyleRef,
    ) {
        try {
            val completed = structureNote(noteId, transcripts, transcriptionMs, progressOffset, style)
            mutableNoteProgress.update { it - noteId }
            if (completed) suggestEventsLocked(noteId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Logger.withTag(LOG_TAG).e(t) { "Processing note $noteId failed" }
            if (failNote(noteId)) {
                mutableEvents.emit(NoteProcessingEvent.Failed(noteId))
            }
        } finally {
            mutableProcessingNoteIds.update { it - noteId }
            mutableNoteProgress.update { it - noteId }
        }
    }

    private suspend fun structureNote(
        noteId: Long,
        transcripts: List<String>,
        transcriptionMs: Long,
        progressOffset: Float,
        style: NoteStyleRef,
    ): Boolean {
        if (transcripts.isEmpty()) {
            failNote(noteId)
            mutableEvents.emit(NoteProcessingEvent.Failed(noteId))
            return false
        }
        persistTranscript(noteId, transcripts, transcriptionMs)
        val structured = structureOrKeepTranscript(noteId, transcripts, progressOffset, style)
        val stillExists = completeNote(
            noteId = noteId,
            title = structured.title,
            body = structured.overview,
            transcript = structured.transcript,
            transcriptionTimeMs = transcriptionMs,
            structuringTimeMs = structured.structuringTimeMs,
            hardwareBackend = structured.hardwareBackend.name,
            style = structured.style,
        )
        if (stillExists) {
            mutableEvents.emit(NoteProcessingEvent.Completed(noteId))
        }
        return stillExists
    }

    private suspend fun structureOrKeepTranscript(
        noteId: Long,
        transcripts: List<String>,
        progressOffset: Float,
        style: NoteStyleRef,
    ): StructuredNote = try {
        transcriptStructurer.structure(transcripts, style) { fraction ->
            updateProgress(noteId, progressOffset + fraction * (1f - progressOffset))
        }
    } catch (backendFailure: AiBackendException) {
        Logger.withTag(LOG_TAG)
            .w(backendFailure) { "Structuring unavailable for note $noteId, keeping transcript" }
        transcriptStructurer.transcriptOnly(transcripts, style)
    }

    private fun addChunk(chunk: AudioChunk) {
        mutableSession.update {
            it.copy(chunks = it.chunks + SessionChunk(chunk.id, chunk.durationMs, ChunkState.QUEUED))
        }
    }

    private fun updateChunk(id: Int, transform: (SessionChunk) -> SessionChunk) {
        mutableSession.update { session ->
            session.copy(chunks = session.chunks.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun fail(message: String) {
        mutableSession.update { it.copy(phase = SessionPhase.FAILED, errorMessage = message) }
    }

    private fun SessionPhase.isActive(): Boolean =
        this == SessionPhase.RECORDING || this == SessionPhase.DRAINING

    private companion object {
        const val LOG_TAG = "RecordingSession"
        // Strictly under the Whisper decoder's 30-second per-decode cap.
        const val RETRY_CHUNK_MS = 29_000L
        const val BYTES_PER_MS = AudioRecorder.SAMPLE_RATE * 2 / 1000L
        const val RETRY_WHISPER_SHARE = 0.6f
        const val IMPORT_DECODE_SHARE = 0.15f
        const val MIN_CHUNK_SPEECH_MS = 1_000L
    }
}
