package com.dmytrosamoilov.offhand.core.audio

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class WavCodecTest {

    @Test
    fun headerMatchesGoldenBytes() {
        val header = WavCodec.header(dataSize = 4, sampleRate = 16_000, channels = 1, bitsPerSample = 16)
        val golden = byteArrayOf(
            0x52, 0x49, 0x46, 0x46,
            0x28, 0x00, 0x00, 0x00,
            0x57, 0x41, 0x56, 0x45,
            0x66, 0x6D, 0x74, 0x20,
            0x10, 0x00, 0x00, 0x00,
            0x01, 0x00,
            0x01, 0x00,
            0x80.toByte(), 0x3E, 0x00, 0x00,
            0x00, 0x7D, 0x00, 0x00,
            0x02, 0x00,
            0x10, 0x00,
            0x64, 0x61, 0x74, 0x61,
            0x04, 0x00, 0x00, 0x00,
        )
        assertContentEquals(golden, header)
    }

    @Test
    fun wrapConcatenatesHeaderAndPcm() {
        val pcm = byteArrayOf(1, 2, 3, 4)
        val wav = WavCodec.wrap(pcm, sampleRate = 16_000, channels = 1, bitsPerSample = 16)
        assertEquals(WavCodec.HEADER_BYTES + pcm.size, wav.size)
        assertContentEquals(WavCodec.header(4, 16_000, 1, 16), wav.copyOfRange(0, WavCodec.HEADER_BYTES))
        assertContentEquals(pcm, wav.copyOfRange(WavCodec.HEADER_BYTES, wav.size))
    }
}
