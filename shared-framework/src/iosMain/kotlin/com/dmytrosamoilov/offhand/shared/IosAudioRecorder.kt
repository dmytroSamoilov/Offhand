package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.audio.AudioChunk
import com.dmytrosamoilov.offhand.core.audio.AudioChunker
import com.dmytrosamoilov.offhand.core.audio.VadSnapshot
import com.dmytrosamoilov.offhand.core.audio.WavCodec
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioRecorder
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

@OptIn(ExperimentalAtomicApi::class)
class IosAudioRecorder(
    private val audioSource: IosAudioSource,
) : AudioRecorder {

    private val paused = AtomicBoolean(false)
    private val finishSession = AtomicReference<(() -> Unit)?>(null)

    private val mutableVad = MutableStateFlow(VadSnapshot())
    override val vad: StateFlow<VadSnapshot> = mutableVad.asStateFlow()

    private val mutableExternalInputName = MutableStateFlow<String?>(null)
    override val externalInputName: StateFlow<String?> = mutableExternalInputName.asStateFlow()

    override fun pause() {
        paused.store(true)
    }

    override fun resume() {
        paused.store(false)
    }

    override fun stop() {
        audioSource.stop()
        finishSession.exchange(null)?.invoke()
    }

    override fun resetVad() {
        mutableVad.value = VadSnapshot()
    }

    override fun recordStream(pcmSink: (ByteArray) -> Unit): Flow<AudioChunk> = callbackFlow {
        val chunker = AudioChunker(
            sampleRate = AudioRecorder.SAMPLE_RATE,
            minChunkMs = MIN_CHUNK_MS,
            maxChunkMs = MAX_CHUNK_MS,
            silenceGapMs = SILENCE_GAP_MS,
            silenceDb = SILENCE_DB,
        )
        paused.store(false)
        mutableVad.value = VadSnapshot()
        mutableExternalInputName.value = null
        val started = audioSource.start(
            onFrame = { frame ->
                val outcome = chunker.onFrame(frame, frame.size, paused.load())
                outcome.frameBytes?.let(pcmSink)
                mutableVad.value = outcome.vad
                outcome.completedChunk?.let { trySend(it.toAudioChunk()) }
            },
            onInputChanged = { name -> mutableExternalInputName.value = name },
            // The capture is already torn down by the time this arrives, so the
            // buffered tail is safe to drain and is worth keeping.
            onFailure = { message ->
                chunker.drainTail()?.let { trySend(it.toAudioChunk()) }
                close(IllegalStateException(message))
            },
        )
        if (!started) {
            close(IllegalStateException("Microphone unavailable"))
            return@callbackFlow
        }
        finishSession.store {
            chunker.drainTail()?.let { trySend(it.toAudioChunk()) }
            close()
        }
        awaitClose {
            audioSource.stop()
            mutableExternalInputName.value = null
        }
    }

    private fun AudioChunker.PcmChunk.toAudioChunk(): AudioChunk = AudioChunk(
        id = id,
        wav = WavCodec.wrap(pcm, AudioRecorder.SAMPLE_RATE, channels = 1, bitsPerSample = 16),
        durationMs = durationMs,
        speechMs = speechMs,
        reason = reason,
    )

    private companion object {
        const val MIN_CHUNK_MS = 20_000L
        const val MAX_CHUNK_MS = 29_000L
        const val SILENCE_GAP_MS = 300L
        const val SILENCE_DB = -45f
    }
}
