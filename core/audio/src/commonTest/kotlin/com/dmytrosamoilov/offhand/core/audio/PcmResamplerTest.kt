package com.dmytrosamoilov.offhand.core.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PcmResamplerTest {

    @Test
    fun `same rate returns the input untouched`() {
        val input = shortArrayOf(1, 2, 3)

        assertSame(input, PcmResampler.resample(input, 16_000, 16_000))
    }

    @Test
    fun `downsampling keeps the duration and the tone`() {
        val inputRate = 48_000
        val seconds = 1
        val tone = ShortArray(inputRate * seconds) { (sin(2 * PI * 440 * it / inputRate) * 10_000).toInt().toShort() }

        val output = PcmResampler.resample(tone, inputRate, 16_000)

        assertEquals(16_000 * seconds, output.size)
        val outputZeroCrossings = output.toList().zipWithNext().count { (a, b) -> a < 0 && b >= 0 }
        assertTrue(abs(outputZeroCrossings - 440) <= 3, "zero crossings $outputZeroCrossings")
        assertTrue(output.maxOf { abs(it.toInt()) } > 6_000)
    }

    @Test
    fun `upsampling interpolates between neighbours`() {
        val output = PcmResampler.resample(shortArrayOf(0, 1000), 8_000, 16_000)

        assertEquals(4, output.size)
        assertEquals(listOf<Short>(0, 500, 1000, 1000), output.toList())
    }

    @Test
    fun `downmix averages the channels`() {
        val stereo = shortArrayOf(100, 300, -200, 200)

        assertEquals(listOf<Short>(200, 0), PcmResampler.downmix(stereo, 2).toList())
        assertSame(stereo, PcmResampler.downmix(stereo, 1))
    }
}
