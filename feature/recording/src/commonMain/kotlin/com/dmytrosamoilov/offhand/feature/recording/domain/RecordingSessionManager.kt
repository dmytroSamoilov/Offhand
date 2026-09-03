package com.dmytrosamoilov.offhand.feature.recording.domain

import co.touchlab.kermit.Logger
import com.dmytrosamoilov.offhand.core.ai.api.AiBackendException
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.ai.api.TranscriptionResult
import com.dmytrosamoilov.offhand.core.audio.AudioChunk
import com.dmytrosamoilov.offhand.core.audio.VadSnapshot
import com.dmytrosamoilov.offhand.core.audio.WavCodec
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
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
    private val markNoteRecorded: MarkNoteRecordedUseCase,
    private val discardNote: DiscardNoteUseCase,
    private val completeNote: CompleteNoteUseCase,
    private val failNote: FailNoteUseCase,
    private val markNoteProcessing: MarkNoteProcessingUseCase,
    private val registerSavedRecording: RegisterSavedRecordingUseCase,
    private val saveNoteTranscript: SaveNoteTranscriptUseCase,
    private val isAiCoreDownloaded: IsAiCoreDownloadedUseCase,
    private val getNotePreset: GetNotePresetUseCase,
    private val getNote: GetNoteUseCase,
    private val audioStore: EncryptedAudioStore,
    private val audioBackup: RecordingAudioBackup,
    private val scope: CoroutineScope,
) {

    // Fair mutex: queued notes are processed one at a time, in arrival order.
    private val processingMutex = Mutex()

    private val mutableSession = MutableStateFlow(RecordingSession())
    val session: StateFlow<RecordingSession> = mutableSession.asStateFlow()

    private val mutableProcessingNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    val processingNoteIds: StateFlow<Set<Long>> = mutableProcessingNoteIds.asStateFlow()

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

    private var sessionPreset = NotePreset.DEFAULT

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

    fun resetToIdle() {
        val phase = mutableSession.value.phase
        if (phase == SessionPhase.FAILED) {
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
                    preset = note.preset,
                )
            }
        }
    }

    fun restructureNote(noteId: Long, preset: NotePreset) {
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
                    preset = preset,
                )
            }
        }
    }

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
        sessionPreset = getNotePreset()
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
        createRecordingNote(audioFileName, sessionPreset)
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
                startProcessing(noteId, chunkTranscripts, transcriptionTimeMs, sessionPreset)
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
        val preset = sessionPreset
        mutableSession.value = RecordingSession()
        startProcessing(noteId, chunkTranscripts, recordedTranscriptionMs, preset)
    }

    private fun startProcessing(
        noteId: Long,
        chunkTranscripts: List<String>,
        transcriptionMs: Long,
        preset: NotePreset,
    ) {
        mutableProcessingNoteIds.update { it + noteId }
        scope.launch {
            if (!isAiCoreDownloaded()) {
                Logger.withTag(LOG_TAG).w { "AI core not downloaded yet, note $noteId stays queued" }
                mutableProcessingNoteIds.update { it - noteId }
                return@launch
            }
            processingMutex.withLock {
                processNote(noteId, chunkTranscripts, transcriptionMs, 0f, preset)
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
        preset: NotePreset,
    ) {
        try {
            if (transcripts.isEmpty()) {
                failNote(noteId)
                mutableEvents.emit(NoteProcessingEvent.Failed(noteId))
                return
            }
            persistTranscript(noteId, transcripts, transcriptionMs)
            val structured = try {
                transcriptStructurer.structure(transcripts, preset) { fraction ->
                    updateProgress(noteId, progressOffset + fraction * (1f - progressOffset))
                }
            } catch (backendFailure: AiBackendException) {
                Logger.withTag(LOG_TAG)
                    .w(backendFailure) { "Structuring unavailable for note $noteId, keeping transcript" }
                transcriptStructurer.transcriptOnly(transcripts)
            }
            val stillExists = completeNote(
                noteId = noteId,
                title = structured.title,
                body = structured.overview,
                transcript = structured.transcript,
                transcriptionTimeMs = transcriptionMs,
                structuringTimeMs = structured.structuringTimeMs,
                hardwareBackend = structured.hardwareBackend.name,
                preset = preset,
            )
            if (stillExists) {
                mutableEvents.emit(NoteProcessingEvent.Completed(noteId))
            }
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
        const val MIN_CHUNK_SPEECH_MS = 1_000L
    }
}
