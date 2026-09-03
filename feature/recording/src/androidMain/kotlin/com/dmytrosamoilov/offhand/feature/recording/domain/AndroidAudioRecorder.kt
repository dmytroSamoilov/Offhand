package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.audio.AudioChunk
import com.dmytrosamoilov.offhand.core.audio.StreamingAudioRecorder
import com.dmytrosamoilov.offhand.core.audio.VadSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class AndroidAudioRecorder(
    private val recorder: StreamingAudioRecorder,
) : AudioRecorder {

    override val vad: StateFlow<VadSnapshot>
        get() = recorder.vad

    override val externalInputName: StateFlow<String?>
        get() = recorder.externalInputName

    override fun pause() = recorder.pause()

    override fun resume() = recorder.resume()

    override fun stop() = recorder.stop()

    override fun resetVad() = recorder.resetVad()

    override fun recordStream(pcmSink: (ByteArray) -> Unit): Flow<AudioChunk> =
        recorder.recordStream(pcmSink = pcmSink)
}
