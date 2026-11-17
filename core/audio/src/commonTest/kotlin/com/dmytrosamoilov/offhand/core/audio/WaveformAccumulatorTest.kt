package com.dmytrosamoilov.offhand.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaveformAccumulatorTest {

    @Test
    fun `a loud half and a silent half give full and empty sections`() {
        val pcm = pcmOf(List(400) { 8000 } + List(400) { 0 })
        val accumulator = WaveformAccumulator(pcm.size.toLong(), sections = 4)

        accumulator.add(pcm, pcm.size)

        assertEquals(listOf(100, 100, 0, 0), accumulator.levels())
    }

    @Test
    fun `a section six decibels below the loudest reads lower in proportion`() {
        val pcm = pcmOf(List(100) { 8000 } + List(100) { 4000 })
        val accumulator = WaveformAccumulator(pcm.size.toLong(), sections = 2)

        accumulator.add(pcm, pcm.size)

        assertEquals(listOf(100, 83), accumulator.levels())
    }

    @Test
    fun `chunks split inside a sample give the same levels as one chunk`() {
        val pcm = pcmOf(List(300) { index -> if (index < 150) -12000 else 3000 })
        val whole = WaveformAccumulator(pcm.size.toLong(), sections = 3)
        val split = WaveformAccumulator(pcm.size.toLong(), sections = 3)

        whole.add(pcm, pcm.size)
        pcm.toList().chunked(7).forEach { chunk -> split.add(chunk.toByteArray(), chunk.size) }

        assertEquals(whole.levels(), split.levels())
    }

    @Test
    fun `silence gives empty sections`() {
        val pcm = pcmOf(List(80) { 0 })
        val accumulator = WaveformAccumulator(pcm.size.toLong(), sections = 4)

        accumulator.add(pcm, pcm.size)

        assertEquals(listOf(0, 0, 0, 0), accumulator.levels())
    }

    @Test
    fun `audio shorter than the sections still fills every level`() {
        val pcm = pcmOf(listOf(5000, 5000))
        val accumulator = WaveformAccumulator(pcm.size.toLong())

        accumulator.add(pcm, pcm.size)

        val levels = accumulator.levels()
        assertEquals(WaveformAccumulator.SECTIONS, levels.size)
        assertTrue(levels.all { it in 0..WaveformAccumulator.MAX_LEVEL })
    }

    private fun pcmOf(samples: List<Int>): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        samples.forEachIndexed { index, sample ->
            bytes[index * 2] = (sample and 0xFF).toByte()
            bytes[index * 2 + 1] = (sample shr 8 and 0xFF).toByte()
        }
        return bytes
    }
}
