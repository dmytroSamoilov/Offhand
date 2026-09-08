package com.dmytrosamoilov.offhand.testing.fakes

import com.dmytrosamoilov.offhand.core.ai.api.SpeechModelState
import com.dmytrosamoilov.offhand.core.ai.api.SpeechToText
import com.dmytrosamoilov.offhand.core.ai.api.TranscriptionResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSpeechToText : SpeechToText {

    override val downloadState: StateFlow<SpeechModelState> =
        MutableStateFlow<SpeechModelState>(SpeechModelState.Downloaded).asStateFlow()

    private var chunkIndex = 0

    override suspend fun prepare() = Unit

    override suspend fun transcribe(audioWav: ByteArray): TranscriptionResult {
        delay(PROCESSING_MS)
        val sentence = SENTENCES[chunkIndex % SENTENCES.size]
        chunkIndex += 1
        return TranscriptionResult(text = sentence, processingTimeMs = PROCESSING_MS)
    }

    override fun release() = Unit

    companion object {
        const val PROCESSING_MS = 300L
        val SENTENCES = listOf(
            "Team meeting about the quarterly budget.",
            "Anna will finish the iPad screenshots by Friday.",
            "Action item: send the release notes to the beta testers.",
        )
    }
}
