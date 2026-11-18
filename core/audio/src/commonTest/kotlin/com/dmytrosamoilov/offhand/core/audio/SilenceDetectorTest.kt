package com.dmytrosamoilov.offhand.core.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

class SilenceDetectorTest {

    @Test
    fun `a two-second pause is skipped with a quarter second kept at each edge`() {
        val detector = detectorFed(speech(1_000) + silence(2_000) + speech(1_000))

        assertEquals(listOf(SilenceSkip(startMs = 1_250, endMs = 2_750)), detector.skips())
    }

    @Test
    fun `a pause shorter than a second is played`() {
        val detector = detectorFed(speech(1_000) + silence(800) + speech(1_000))

        assertEquals(emptyList(), detector.skips())
    }

    @Test
    fun `room noise between words counts as silence`() {
        val detector = detectorFed(speech(1_000) + noise(1_500) + speech(1_000))

        assertEquals(listOf(SilenceSkip(startMs = 1_250, endMs = 2_250)), detector.skips())
    }

    @Test
    fun `steady speech has nothing to skip`() {
        assertEquals(emptyList(), detectorFed(speech(3_000)).skips())
    }

    @Test
    fun `chunks split inside a sample find the same pauses`() {
        val pcm = speech(1_000) + silence(1_500) + speech(500)
        val split = SilenceDetector()
        pcm.toList().chunked(333).forEach { chunk -> split.add(chunk.toByteArray(), chunk.size) }

        assertEquals(detectorFed(pcm).skips(), split.skips())
    }

    private fun detectorFed(pcm: ByteArray) = SilenceDetector().apply { add(pcm, pcm.size) }

    private fun speech(ms: Int) = tone(ms, amplitude = 8_000)

    private fun noise(ms: Int) = tone(ms, amplitude = 30)

    private fun silence(ms: Int) = ByteArray(ms * BYTES_PER_MS)

    private fun tone(ms: Int, amplitude: Int): ByteArray {
        val samples = ms * SAMPLES_PER_MS
        val bytes = ByteArray(samples * 2)
        repeat(samples) { index ->
            val sample = (sin(2 * PI * 220 * index / 16_000) * amplitude).toInt()
            bytes[index * 2] = (sample and 0xFF).toByte()
            bytes[index * 2 + 1] = (sample shr 8 and 0xFF).toByte()
        }
        return bytes
    }

    private companion object {
        const val SAMPLES_PER_MS = 16
        const val BYTES_PER_MS = SAMPLES_PER_MS * 2
    }
}
