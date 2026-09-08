package com.dmytrosamoilov.offhand.testing.fakes

import com.dmytrosamoilov.offhand.core.audio.AudioChunk
import com.dmytrosamoilov.offhand.core.audio.ChunkBoundaryReason
import com.dmytrosamoilov.offhand.core.audio.VadSnapshot
import com.dmytrosamoilov.offhand.core.audio.WavCodec
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioRecorder
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

class FakeAudioRecorder : AudioRecorder {

    private val mutableVad = MutableStateFlow(VadSnapshot())
    override val vad: StateFlow<VadSnapshot> = mutableVad.asStateFlow()

    override val externalInputName: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()

    private val paused = MutableStateFlow(false)
    private val stopRequested = MutableStateFlow(false)

    override fun pause() {
        paused.value = true
    }

    override fun resume() {
        paused.value = false
    }

    override fun stop() {
        stopRequested.value = true
    }

    override fun resetVad() {
        mutableVad.value = VadSnapshot()
    }

    override fun recordStream(pcmSink: (ByteArray) -> Unit): Flow<AudioChunk> = flow {
        stopRequested.value = false
        paused.value = false
        val session = ToneSession()
        while (!stopRequested.value) {
            delay(FRAME_MS)
            if (paused.value) {
                mutableVad.value = session.pausedSnapshot()
                continue
            }
            pcmSink(session.nextFrame())
            mutableVad.value = session.snapshot()
            if (session.chunkMs >= CHUNK_MS) emit(session.completeChunk(ChunkBoundaryReason.MAX_DURATION))
        }
        if (session.chunkMs > 0) emit(session.completeChunk(ChunkBoundaryReason.USER_STOP))
    }

    private class ToneSession {
        private val frames = mutableListOf<ByteArray>()
        private var chunkId = 1
        private var totalMs = 0L
        private var phase = 0.0
        var chunkMs = 0L
            private set

        fun nextFrame(): ByteArray {
            val samples = (AudioRecorder.SAMPLE_RATE * FRAME_MS / MS_PER_SECOND).toInt()
            val bytes = ByteArray(samples * BYTES_PER_SAMPLE)
            repeat(samples) { index ->
                val sample = (sin(phase) * AMPLITUDE).toInt()
                phase += PHASE_STEP
                bytes[index * 2] = (sample and 0xff).toByte()
                bytes[index * 2 + 1] = ((sample ushr 8) and 0xff).toByte()
            }
            frames += bytes
            chunkMs += FRAME_MS
            totalMs += FRAME_MS
            return bytes
        }

        fun snapshot(): VadSnapshot = VadSnapshot(
            rmsDb = TONE_DB,
            isSilent = false,
            chunkElapsedMs = chunkMs,
            totalElapsedMs = totalMs,
            currentChunkId = chunkId,
        )

        fun pausedSnapshot(): VadSnapshot = snapshot().copy(
            rmsDb = VadSnapshot.SILENCE_DB,
            isSilent = true,
            isPaused = true,
        )

        fun completeChunk(reason: ChunkBoundaryReason): AudioChunk {
            val pcm = ByteArray(frames.sumOf { it.size })
            var offset = 0
            frames.forEach { frame ->
                frame.copyInto(pcm, offset)
                offset += frame.size
            }
            val chunk = AudioChunk(
                id = chunkId,
                wav = WavCodec.wrap(pcm, AudioRecorder.SAMPLE_RATE, channels = 1, bitsPerSample = 16),
                durationMs = chunkMs,
                speechMs = chunkMs,
                reason = reason,
            )
            frames.clear()
            chunkId += 1
            chunkMs = 0L
            return chunk
        }
    }

    private companion object {
        const val FRAME_MS = 100L
        const val CHUNK_MS = 3_000L
        const val MS_PER_SECOND = 1_000L
        const val BYTES_PER_SAMPLE = 2
        const val AMPLITUDE = 8_000.0
        const val TONE_HZ = 440.0
        const val TONE_DB = -15f
        val PHASE_STEP = 2 * PI * TONE_HZ / AudioRecorder.SAMPLE_RATE
    }
}
