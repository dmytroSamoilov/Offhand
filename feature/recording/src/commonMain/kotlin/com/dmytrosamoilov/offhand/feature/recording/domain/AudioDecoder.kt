package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource

data class DecodedAudio(
    val durationMs: Long,
)

interface AudioDecoder {

    suspend fun decode(
        source: AudioImportSource,
        onPcm: (bytes: ByteArray, length: Int) -> Unit,
        onProgress: (Float) -> Unit,
    ): DecodedAudio

    fun discard(source: AudioImportSource)
}

sealed class AudioImportException(message: String) : Exception(message) {
    class Unsupported : AudioImportException("The audio format is not supported")
    class TooLong(val durationMs: Long) : AudioImportException("The recording is longer than the import limit")
    class Unreadable(cause: Throwable?) : AudioImportException("The audio file could not be read: ${cause?.message}")
}

object AudioImportLimits {
    const val MAX_DURATION_MS = 2L * 60 * 60 * 1000
}
