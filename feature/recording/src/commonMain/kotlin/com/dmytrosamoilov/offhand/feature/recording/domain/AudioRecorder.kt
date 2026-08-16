package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.audio.AudioChunk
import com.dmytrosamoilov.offhand.core.audio.VadSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AudioRecorder {

    val vad: StateFlow<VadSnapshot>

    val externalInputName: StateFlow<String?>

    fun pause()

    fun resume()

    fun stop()

    fun resetVad()

    fun recordStream(pcmSink: (ByteArray) -> Unit): Flow<AudioChunk>

    companion object {
        const val SAMPLE_RATE = 16_000
    }
}
