package com.dmytrosamoilov.offhand.core.audio

import kotlin.math.log10
import kotlin.math.sqrt

class AudioChunker(
    private val sampleRate: Int,
    private val minChunkMs: Long,
    private val maxChunkMs: Long,
    private val silenceGapMs: Long,
    private val silenceDb: Float,
) {

    class FrameOutcome(
        val vad: VadSnapshot,
        val frameBytes: ByteArray?,
        val completedChunk: PcmChunk?,
    )

    class PcmChunk(
        val id: Int,
        val pcm: ByteArray,
        val durationMs: Long,
        val speechMs: Long,
        val reason: ChunkBoundaryReason,
    )

    private val frames = mutableListOf<ByteArray>()
    private var bufferedBytes = 0
    private var chunkId = 1
    private var chunkRecordedMs = 0L
    private var chunkSpeechMs = 0L
    private var totalRecordedMs = 0L
    private var consecutiveSilentMs = 0L

    fun onFrame(frame: ShortArray, count: Int, isPaused: Boolean): FrameOutcome {
        if (isPaused) {
            consecutiveSilentMs = 0L
            return FrameOutcome(pausedSnapshot(), frameBytes = null, completedChunk = null)
        }
        val rmsDb = rmsDb(frame, count)
        val isSilent = rmsDb < silenceDb
        val frameBytes = toLittleEndianBytes(frame, count)
        frames += frameBytes
        bufferedBytes += frameBytes.size

        val frameMs = (count.toLong() * MS_PER_SECOND) / sampleRate
        if (isSilent) {
            consecutiveSilentMs += frameMs
        } else {
            consecutiveSilentMs = 0L
            chunkSpeechMs += frameMs
        }
        chunkRecordedMs += frameMs
        totalRecordedMs += frameMs

        val vad = VadSnapshot(
            rmsDb = rmsDb,
            isSilent = isSilent,
            chunkElapsedMs = chunkRecordedMs,
            totalElapsedMs = totalRecordedMs,
            currentChunkId = chunkId,
        )
        val reason = when {
            chunkRecordedMs >= maxChunkMs -> ChunkBoundaryReason.MAX_DURATION
            chunkRecordedMs >= minChunkMs && consecutiveSilentMs >= silenceGapMs ->
                ChunkBoundaryReason.SILENCE_GAP

            else -> null
        }
        return FrameOutcome(vad, frameBytes, reason?.let { completeChunk(chunkRecordedMs, it) })
    }

    fun drainTail(): PcmChunk? {
        if (bufferedBytes == 0) return null
        val tailMs = (bufferedBytes.toLong() / BYTES_PER_SAMPLE * MS_PER_SECOND) / sampleRate
        return completeChunk(tailMs, ChunkBoundaryReason.USER_STOP)
    }

    private fun completeChunk(durationMs: Long, reason: ChunkBoundaryReason): PcmChunk {
        val chunk = PcmChunk(
            id = chunkId,
            pcm = joinFrames(),
            durationMs = durationMs,
            speechMs = chunkSpeechMs,
            reason = reason,
        )
        frames.clear()
        bufferedBytes = 0
        chunkId += 1
        chunkRecordedMs = 0L
        chunkSpeechMs = 0L
        consecutiveSilentMs = 0L
        return chunk
    }

    private fun joinFrames(): ByteArray {
        val joined = ByteArray(bufferedBytes)
        var offset = 0
        frames.forEach { frame ->
            frame.copyInto(joined, offset)
            offset += frame.size
        }
        return joined
    }

    private fun pausedSnapshot(): VadSnapshot = VadSnapshot(
        rmsDb = VadSnapshot.SILENCE_DB,
        isSilent = true,
        isPaused = true,
        chunkElapsedMs = chunkRecordedMs,
        totalElapsedMs = totalRecordedMs,
        currentChunkId = chunkId,
    )

    private fun rmsDb(frame: ShortArray, count: Int): Float {
        if (count <= 0) return VadSnapshot.SILENCE_DB
        var sumOfSquares = 0.0
        for (i in 0 until count) {
            val normalized = frame[i].toDouble() / PCM_FULL_SCALE
            sumOfSquares += normalized * normalized
        }
        val rms = sqrt(sumOfSquares / count)
        if (rms <= MIN_MEASURABLE_RMS) return VadSnapshot.SILENCE_DB
        return (DB_FACTOR * log10(rms)).toFloat()
    }

    private fun toLittleEndianBytes(frame: ShortArray, count: Int): ByteArray {
        val bytes = ByteArray(count * BYTES_PER_SAMPLE)
        for (i in 0 until count) {
            val sample = frame[i].toInt()
            bytes[i * 2] = (sample and 0xff).toByte()
            bytes[i * 2 + 1] = ((sample ushr 8) and 0xff).toByte()
        }
        return bytes
    }

    private companion object {
        const val MS_PER_SECOND = 1_000L
        const val BYTES_PER_SAMPLE = 2
        const val PCM_FULL_SCALE = 32_768.0
        const val MIN_MEASURABLE_RMS = 1e-9
        const val DB_FACTOR = 20.0
    }
}
