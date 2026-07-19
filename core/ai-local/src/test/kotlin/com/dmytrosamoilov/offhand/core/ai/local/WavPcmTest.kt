package com.dmytrosamoilov.offhand.core.ai.local

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WavPcmTest {

    private fun wavOf(vararg samples: Short): ByteArray {
        val dataSize = samples.size * Short.SIZE_BYTES
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(1)
        buffer.putInt(16_000)
        buffer.putInt(32_000)
        buffer.putShort(2)
        buffer.putShort(16)
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)
        samples.forEach { buffer.putShort(it) }
        return buffer.array()
    }

    @Test
    fun `converts pcm16 samples to normalized floats`() {
        val samples = WavPcm.toFloatSamples(wavOf(0, 16384, -16384, 32767, -32768))

        assertEquals(5, samples.size)
        assertEquals(0f, samples[0], 0.0001f)
        assertEquals(0.5f, samples[1], 0.0001f)
        assertEquals(-0.5f, samples[2], 0.0001f)
        assertEquals(1f, samples[3], 0.001f)
        assertEquals(-1f, samples[4], 0.0001f)
    }

    @Test
    fun `rejects non-wav payload`() {
        assertThrows(IllegalArgumentException::class.java) {
            WavPcm.toFloatSamples(ByteArray(64) { 1 })
        }
    }

    @Test
    fun `rejects wav without data chunk`() {
        val header = wavOf(0).copyOfRange(0, 36)
        assertThrows(IllegalArgumentException::class.java) {
            WavPcm.toFloatSamples(header)
        }
    }
}
