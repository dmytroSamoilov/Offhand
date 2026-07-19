package com.dmytrosamoilov.offhand.core.ai.api

data class TranscriptionResult(
    val text: String,
    val processingTimeMs: Long,
)

interface SpeechToText {

    suspend fun prepare()

    suspend fun transcribe(audioWav: ByteArray): TranscriptionResult

    fun release()
}
