package com.dmytrosamoilov.offhand.core.audio

import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Loudness of a recording in equal sections, from 16-bit little-endian mono
// PCM fed in chunks of any size. Each level is the section's RMS in decibels
// below the loudest section, mapped onto 0..MAX_LEVEL across RANGE_DB, so a
// quiet stretch reads as low whatever the recording's overall volume.
class WaveformAccumulator(totalBytes: Long, private val sections: Int = SECTIONS) {

    private val totalSamples = (totalBytes / BYTES_PER_SAMPLE).coerceAtLeast(1L)
    private val sumOfSquares = DoubleArray(sections)
    private val counts = LongArray(sections)
    private var sampleIndex = 0L
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

    fun levels(): List<Int> {
        val rms = List(sections) { section -> rmsOf(section) }
        val loudest = rms.max()
        if (loudest <= 0.0) return List(sections) { 0 }
        return rms.map { value -> levelOf(value, loudest) }
    }

    private fun addSample(sample: Int) {
        val section = (sampleIndex * sections / totalSamples).toInt().coerceAtMost(sections - 1)
        sumOfSquares[section] += sample.toDouble() * sample
        counts[section]++
        sampleIndex++
    }

    private fun rmsOf(section: Int): Double =
        if (counts[section] == 0L) 0.0 else sqrt(sumOfSquares[section] / counts[section])

    private fun levelOf(rms: Double, loudest: Double): Int {
        if (rms <= 0.0) return 0
        val decibelsBelow = DECIBELS_PER_DECADE * log10(rms / loudest)
        return ((1.0 + decibelsBelow / RANGE_DB).coerceIn(0.0, 1.0) * MAX_LEVEL).roundToInt()
    }

    companion object {
        const val SECTIONS = 40
        const val MAX_LEVEL = 100
        private const val RANGE_DB = 36.0
        private const val DECIBELS_PER_DECADE = 20.0
        private const val BYTES_PER_SAMPLE = 2L
        private const val BITS_PER_BYTE = 8
        private const val BYTE_MASK = 0xFF
        private const val NO_BYTE = -1
    }
}
