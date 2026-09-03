package com.dmytrosamoilov.offhand.core.audio

object WavCodec {

    const val HEADER_BYTES = 44

    fun header(
        dataSize: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val header = ByteArray(HEADER_BYTES)
        var offset = 0
        offset = header.putAscii(offset, "RIFF")
        offset = header.putIntLe(offset, 36 + dataSize)
        offset = header.putAscii(offset, "WAVE")
        offset = header.putAscii(offset, "fmt ")
        offset = header.putIntLe(offset, 16)
        offset = header.putShortLe(offset, 1)
        offset = header.putShortLe(offset, channels)
        offset = header.putIntLe(offset, sampleRate)
        offset = header.putIntLe(offset, byteRate)
        offset = header.putShortLe(offset, blockAlign)
        offset = header.putShortLe(offset, bitsPerSample)
        offset = header.putAscii(offset, "data")
        header.putIntLe(offset, dataSize)
        return header
    }

    fun wrap(
        pcm: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
    ): ByteArray {
        val wav = ByteArray(HEADER_BYTES + pcm.size)
        header(pcm.size, sampleRate, channels, bitsPerSample).copyInto(wav)
        pcm.copyInto(wav, HEADER_BYTES)
        return wav
    }

    private fun ByteArray.putAscii(offset: Int, text: String): Int {
        text.forEachIndexed { index, char -> this[offset + index] = char.code.toByte() }
        return offset + text.length
    }

    private fun ByteArray.putIntLe(offset: Int, value: Int): Int {
        this[offset] = (value and 0xff).toByte()
        this[offset + 1] = ((value ushr 8) and 0xff).toByte()
        this[offset + 2] = ((value ushr 16) and 0xff).toByte()
        this[offset + 3] = ((value ushr 24) and 0xff).toByte()
        return offset + 4
    }

    private fun ByteArray.putShortLe(offset: Int, value: Int): Int {
        this[offset] = (value and 0xff).toByte()
        this[offset + 1] = ((value ushr 8) and 0xff).toByte()
        return offset + 2
    }
}
