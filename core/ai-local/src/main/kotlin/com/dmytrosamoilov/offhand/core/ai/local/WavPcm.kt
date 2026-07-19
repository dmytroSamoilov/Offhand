package com.dmytrosamoilov.offhand.core.ai.local

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object WavPcm {

    fun toFloatSamples(wav: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        require(wav.size >= HEADER_MIN_BYTES && readTag(buffer) == "RIFF") { "Not a RIFF file" }
        buffer.int
        require(readTag(buffer) == "WAVE") { "Not a WAVE file" }
        while (buffer.remaining() >= CHUNK_HEADER_BYTES) {
            val chunkId = readTag(buffer)
            val chunkSize = buffer.int
            if (chunkId == "data") {
                return readPcm16(buffer, chunkSize.coerceAtMost(buffer.remaining()))
            }
            buffer.position(buffer.position() + chunkSize.coerceAtMost(buffer.remaining()))
        }
        throw IllegalArgumentException("WAVE data chunk not found")
    }

    private fun readPcm16(buffer: ByteBuffer, byteCount: Int): FloatArray {
        val samples = FloatArray(byteCount / Short.SIZE_BYTES)
        for (index in samples.indices) {
            samples[index] = buffer.short / PCM16_MAX
        }
        return samples
    }

    private fun readTag(buffer: ByteBuffer): String {
        val bytes = ByteArray(TAG_BYTES)
        buffer.get(bytes)
        return String(bytes, Charsets.US_ASCII)
    }

    private const val PCM16_MAX = 32768f
    private const val TAG_BYTES = 4
    private const val CHUNK_HEADER_BYTES = 8
    private const val HEADER_MIN_BYTES = 12
}
