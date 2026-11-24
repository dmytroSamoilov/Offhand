package com.dmytrosamoilov.offhand.core.audio

import kotlin.math.log10
import kotlin.math.sqrt

data class SilenceSkip(val startMs: Long, val endMs: Long)

// Finds the long pauses in a recording (16 kHz, 16-bit little-endian mono PCM
// fed in chunks of any size) so playback can jump over them. The level that
// counts as silence follows the recording's own background noise: the quiet
// end of its 50 ms frames plus a margin, held between two bounds so a noisy
// room or a near-silent one cannot push it into speech. Only a pause of at
// least MIN_PAUSE_MS is skipped, and EDGE_MS of it is kept at each end so
// words are not clipped and the jump does not feel abrupt.
class SilenceDetector {

    private val frameDecibels = ArrayList<Double>()
    private var frameSumOfSquares = 0.0
    private var frameSamples = 0
    private var pendingLowByte = NO_BYTE

    fun add(bytes: ByteArray, length: Int) {
        for (index in 0 until length) {
            val byte = bytes[index].toInt() and BYTE_MASK
            if (pendingLowByte == NO_BYTE) {
                pendingLowByte = byte
            } else {
                addSample(((byte shl BITS_PER_BYTE) or pendingLowByte).toShort().toInt())
                pendingLowByte = NO_BYTE
            }
        }
    }

    fun skips(): List<SilenceSkip> {
        if (frameDecibels.isEmpty()) return emptyList()
        val threshold = silenceThreshold()
        return quietRuns(threshold).mapNotNull { (first, last) -> skipOf(first, last) }
    }

    private fun addSample(sample: Int) {
        frameSumOfSquares += sample.toDouble() * sample
        frameSamples++
        if (frameSamples == SAMPLES_PER_FRAME) {
            frameDecibels += decibelsOf(sqrt(frameSumOfSquares / frameSamples))
            frameSumOfSquares = 0.0
            frameSamples = 0
        }
    }

    private fun silenceThreshold(): Double {
        val sorted = frameDecibels.sorted()
        val noiseFloor = sorted[(sorted.size * NOISE_FLOOR_PERCENTILE).toInt().coerceAtMost(sorted.lastIndex)]
        return (noiseFloor + MARGIN_DB).coerceIn(LOWEST_THRESHOLD_DB, HIGHEST_THRESHOLD_DB)
    }

    private fun quietRuns(threshold: Double): List<Pair<Int, Int>> {
        val runs = mutableListOf<Pair<Int, Int>>()
        var runStart = -1
        frameDecibels.forEachIndexed { index, decibels ->
            if (decibels < threshold) {
                if (runStart < 0) runStart = index
            } else if (runStart >= 0) {
                runs += runStart to index - 1
                runStart = -1
            }
        }
        if (runStart >= 0) runs += runStart to frameDecibels.lastIndex
        return runs
    }

    private fun skipOf(firstFrame: Int, lastFrame: Int): SilenceSkip? {
        val startMs = firstFrame * FRAME_MS
        val endMs = (lastFrame + 1) * FRAME_MS
        if (endMs - startMs < MIN_PAUSE_MS) return null
        return SilenceSkip(startMs = startMs + EDGE_MS, endMs = endMs - EDGE_MS)
    }

    private fun decibelsOf(rms: Double): Double =
        if (rms <= 0.0) SILENT_DB else DECIBELS_PER_DECADE * log10(rms / FULL_SCALE)

    companion object {
        const val FRAME_MS = 50L
        const val MIN_PAUSE_MS = 1_000L
        const val EDGE_MS = 250L
        private const val SAMPLE_RATE = 16_000
        private const val SAMPLES_PER_FRAME = (SAMPLE_RATE * FRAME_MS / 1_000).toInt()
        private const val NOISE_FLOOR_PERCENTILE = 0.1
        private const val MARGIN_DB = 12.0
        private const val LOWEST_THRESHOLD_DB = -60.0
        private const val HIGHEST_THRESHOLD_DB = -38.0
        private const val SILENT_DB = -120.0
        private const val FULL_SCALE = 32_768.0
        private const val DECIBELS_PER_DECADE = 20.0
        private const val BITS_PER_BYTE = 8
        private const val BYTE_MASK = 0xFF
        private const val NO_BYTE = -1
    }
}
