package com.dmytrosamoilov.offhand.core.ai.api

import kotlinx.coroutines.flow.StateFlow

data class TranscriptionResult(
    val text: String,
    val processingTimeMs: Long,
)

interface SpeechToText {

    val downloadState: StateFlow<SpeechModelState>

    suspend fun prepare()

    suspend fun transcribe(audioWav: ByteArray): TranscriptionResult

    fun release()
}
