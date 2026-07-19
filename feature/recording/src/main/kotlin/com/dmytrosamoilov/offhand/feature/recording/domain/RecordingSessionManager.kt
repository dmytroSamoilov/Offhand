package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.audio.AudioChunk
import com.dmytrosamoilov.offhand.core.audio.StreamingAudioRecorder
import com.dmytrosamoilov.offhand.core.audio.VadSnapshot
import com.dmytrosamoilov.offhand.core.audio.WavCodec
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.feature.recording.di.RecordingSessionScope
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CompleteNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.CreateProcessingNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.FailNoteUseCase
import com.dmytrosamoilov.offhand.feature.recording.domain.usecase.MarkNoteProcessingUseCase
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
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
import timber.log.Timber

@Singleton
class RecordingSessionManager @Inject constructor(
    private val recorder: StreamingAudioRecorder,
    private val speechToText: SpeechToText,
    private val transcriptProofreader: TranscriptProofreader,
    private val transcriptStructurer: TranscriptStructurer,
    private val createProcessingNote: CreateProcessingNoteUseCase,
    private val completeNote: CompleteNoteUseCase,
    private val failNote: FailNoteUseCase,
    private val markNoteProcessing: MarkNoteProcessingUseCase,
    private val audioStore: EncryptedAudioStore,
    @RecordingSessionScope private val scope: CoroutineScope,
) {

    private val mutableSession = MutableStateFlow(RecordingSession())
    val session: StateFlow<RecordingSession> = mutableSession.asStateFlow()

    private val mutableProcessingNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    val processingNoteIds: StateFlow<Set<Long>> = mutableProcessingNoteIds.asStateFlow()

    private val mutableNoteProgress = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val noteProgress: StateFlow<Map<Long, Int>> = mutableNoteProgress.asStateFlow()

    private val mutableEvents = MutableSharedFlow<NoteProcessingEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<NoteProcessingEvent> = mutableEvents.asSharedFlow()

    val vad: StateFlow<VadSnapshot> = recorder.vad

    val externalMicName: StateFlow<String?> = recorder.externalInputName

    private val transcripts = mutableMapOf<Int, String>()
    private var transcriptionTimeMs = 0L

    @Volatile
    private var audioFileName: String? = null

    @Volatile
    private var audioStream: OutputStream? = null

    @Volatile
    private var isDiscardRequested = false

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
            if (!markNoteProcessing(noteId)) {
                mutableProcessingNoteIds.update { it - noteId }
                return@launch
            }
            updateProgress(noteId, 0f)
            val stored = transcribeStoredAudio(audioFileName) { fraction ->
                updateProgress(noteId, fraction * RETRY_WHISPER_SHARE)
            }
            processNote(noteId, stored.texts, stored.transcriptionTimeMs, RETRY_WHISPER_SHARE)
        }
    }

    private suspend fun transcribeStoredAudio(
        audioFileName: String,
        onProgress: (Float) -> Unit,
    ): StoredTranscription = try {
        speechToText.prepare()
        val pcm = audioStore.openForRead(audioFileName).use { it.readBytes() }
        transcribePcmChunks(pcm, onProgress)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (t: Throwable) {
        Timber.tag(LOG_TAG).e(t, "Stored audio transcription failed")
        StoredTranscription(texts = emptyList(), transcriptionTimeMs = 0)
    } finally {
        speechToText.release()
    }

    private suspend fun transcribePcmChunks(
        pcm: ByteArray,
        onProgress: (Float) -> Unit,
    ): StoredTranscription {
        val chunkBytes = (RETRY_CHUNK_MS * BYTES_PER_MS).toInt()
        val texts = mutableListOf<String>()
        var totalTimeMs = 0L
        var offset = 0
        while (offset < pcm.size) {
            val end = minOf(offset + chunkBytes, pcm.size)
            val wav = WavCodec.wrap(
                pcm = pcm.copyOfRange(offset, end),
                sampleRate = StreamingAudioRecorder.SAMPLE_RATE,
                channels = 1,
                bitsPerSample = 16,
            )
            val result = speechToText.transcribe(wav)
            totalTimeMs += result.processingTimeMs
            result.text.trim().takeIf { it.isNotBlank() }?.let(texts::add)
            offset = end
            onProgress(end / pcm.size.toFloat())
        }
        return StoredTranscription(texts = texts, transcriptionTimeMs = totalTimeMs)
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
        openAudioBackup()
        val queue = Channel<AudioChunk>(Channel.UNLIMITED)
        scope.launch { prepareTranscriber() }
        scope.launch { produceChunks(queue) }
        for (chunk in queue) {
            if (!isDiscardRequested) {
                transcribeChunk(chunk)
            }
        }
        speechToText.release()
        if (isDiscardRequested) {
            finishDiscardedSession()
            return
        }
        if (mutableSession.value.phase == SessionPhase.FAILED) return
        launchNoteProcessing()
    }

    private fun finishDiscardedSession() {
        transcripts.clear()
        isDiscardRequested = false
        mutableSession.value = RecordingSession()
    }

    private suspend fun prepareTranscriber() {
        try {
            speechToText.prepare()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Timber.tag(LOG_TAG).w(t, "Transcriber warm-up failed, retrying per chunk")
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
                createPlaceholderNote()
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Timber.tag(LOG_TAG).e(t, "Recording failed")
            closeAudioBackup()
            fail(t.message ?: "Recording failed")
        } finally {
            queue.close()
        }
    }

    private suspend fun createPlaceholderNote() {
        val noteId = createProcessingNote(audioFileName, recorder.vad.value.totalElapsedMs)
        mutableSession.update { it.copy(noteId = noteId) }
    }

    private fun deleteAudioBackup() {
        audioFileName?.let { fileName -> runCatching { audioStore.delete(fileName) } }
        audioFileName = null
    }

    private fun openAudioBackup() {
        try {
            val fileName = audioStore.newRecordingFileName()
            audioStream = BufferedOutputStream(audioStore.openForWrite(fileName))
            audioFileName = fileName
        } catch (t: Throwable) {
            Timber.tag(LOG_TAG).w(t, "Audio backup unavailable, recording without it")
            audioStream = null
            audioFileName = null
        }
    }

    private fun writeAudioFrame(pcm: ByteArray) {
        val stream = audioStream ?: return
        try {
            stream.write(pcm)
        } catch (io: IOException) {
            Timber.tag(LOG_TAG).w(io, "Audio backup write failed, dropping the file")
            runCatching { stream.close() }
            audioStream = null
            audioFileName?.let { fileName -> runCatching { audioStore.delete(fileName) } }
            audioFileName = null
        }
    }

    private fun closeAudioBackup() {
        audioStream?.let { stream ->
            runCatching { stream.close() }
                .onFailure { Timber.tag(LOG_TAG).w(it, "Audio backup close failed") }
        }
        audioStream = null
    }

    private suspend fun transcribeChunk(chunk: AudioChunk) {
        updateChunk(chunk.id) { it.copy(state = ChunkState.TRANSCRIBING) }
        try {
            val result = speechToText.transcribe(chunk.wav)
            transcripts[chunk.id] = result.text.trim()
            transcriptionTimeMs += result.processingTimeMs
            mutableSession.update { it.copy(transcriptionTimeMs = transcriptionTimeMs) }
            updateChunk(chunk.id) { it.copy(state = ChunkState.DONE) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Timber.tag(LOG_TAG).w(t, "Chunk %d transcription failed", chunk.id)
            updateChunk(chunk.id) { it.copy(state = ChunkState.FAILED) }
        }
    }

    private fun launchNoteProcessing() {
        val noteId = mutableSession.value.noteId
        if (noteId == null) {
            fail("Could not save the recording")
            return
        }
        val chunkTranscripts = transcripts.toSortedMap().values.filter { it.isNotBlank() }
        val recordedTranscriptionMs = transcriptionTimeMs
        mutableProcessingNoteIds.update { it + noteId }
        mutableSession.value = RecordingSession()
        scope.launch {
            processNote(noteId, chunkTranscripts, recordedTranscriptionMs, progressOffset = 0f)
        }
    }

    private suspend fun processNote(
        noteId: Long,
        transcripts: List<String>,
        transcriptionMs: Long,
        progressOffset: Float,
    ) {
        try {
            if (transcripts.isEmpty()) {
                failNote(noteId)
                mutableEvents.emit(NoteProcessingEvent.Failed(noteId))
                return
            }
            val proofread = transcriptProofreader.proofread(transcripts) { fraction ->
                val share = fraction * PROOFREAD_PROGRESS_SHARE
                updateProgress(noteId, progressOffset + share * (1f - progressOffset))
            }
            val structuringOffset = progressOffset + PROOFREAD_PROGRESS_SHARE * (1f - progressOffset)
            val structured = transcriptStructurer.structure(proofread.chunks) { fraction ->
                updateProgress(noteId, structuringOffset + fraction * (1f - structuringOffset))
            }
            val stillExists = completeNote(
                noteId = noteId,
                title = structured.title,
                body = structured.overview,
                transcript = structured.transcript,
                transcriptionTimeMs = transcriptionMs,
                structuringTimeMs = proofread.processingTimeMs + structured.structuringTimeMs,
                hardwareBackend = structured.hardwareBackend.name,
            )
            if (stillExists) {
                mutableEvents.emit(NoteProcessingEvent.Completed(noteId))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Timber.tag(LOG_TAG).e(t, "Processing note %d failed", noteId)
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
        const val RETRY_CHUNK_MS = 30_000L
        const val BYTES_PER_MS = StreamingAudioRecorder.SAMPLE_RATE * 2 / 1000L
        const val RETRY_WHISPER_SHARE = 0.6f
        const val PROOFREAD_PROGRESS_SHARE = 0.5f
    }
}
