package com.dmytrosamoilov.offhand.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

data class PcmPlaybackState(
    val isLoaded: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
)

class PcmAudioPlayer @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val mutableState = MutableStateFlow(PcmPlaybackState())
    val state: StateFlow<PcmPlaybackState> = mutableState.asStateFlow()

    @Volatile
    private var channel: SeekableByteChannel? = null

    @Volatile
    private var audioTrack: AudioTrack? = null

    private var feedJob: Job? = null

    fun load(openChannel: () -> SeekableByteChannel) {
        reset()
        scope.launch {
            runCatching {
                val opened = openChannel()
                channel = opened
                // Tink's seekable decrypter can't report size() until the first
                // read initializes the stream header.
                opened.read(ByteBuffer.allocate(1))
                val durationMs = bytesToMs(opened.size())
                opened.position(0)
                mutableState.value = PcmPlaybackState(
                    isLoaded = true,
                    durationMs = durationMs,
                )
            }.onFailure { failure ->
                Timber.tag(LOG_TAG).w(failure, "Failed to open recording for playback")
                mutableState.value = PcmPlaybackState()
            }
        }
    }

    fun play() {
        val current = mutableState.value
        if (!current.isLoaded || current.isPlaying) return
        val track = audioTrack ?: createTrack().also { audioTrack = it }
        runCatching { track.play() }.onFailure { return }
        mutableState.update { it.copy(isPlaying = true) }
        feedJob = scope.launch { feedLoop(track) }
    }

    fun pause() {
        feedJob?.cancel()
        feedJob = null
        runCatching { audioTrack?.pause() }
        mutableState.update { it.copy(isPlaying = false) }
    }

    fun seekTo(positionMs: Long) {
        val opened = channel ?: return
        val wasPlaying = mutableState.value.isPlaying
        feedJob?.cancel()
        feedJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
        }
        val clamped = positionMs.coerceIn(0, mutableState.value.durationMs)
        runCatching { opened.position(msToBytes(clamped)) }
        mutableState.update { it.copy(positionMs = clamped, isPlaying = false) }
        if (wasPlaying) play()
    }

    fun reset() {
        feedJob?.cancel()
        feedJob = null
        runCatching { audioTrack?.pause() }
        runCatching { audioTrack?.flush() }
        runCatching { audioTrack?.release() }
        audioTrack = null
        runCatching { channel?.close() }
        channel = null
        mutableState.value = PcmPlaybackState()
    }

    fun release() {
        reset()
        scope.cancel()
    }

    private suspend fun feedLoop(track: AudioTrack) {
        val opened = channel ?: return
        val buffer = ByteBuffer.allocate(CHUNK_BYTES)
        while (currentCoroutineContext().isActive) {
            buffer.clear()
            val read = runCatching { opened.read(buffer) }.getOrDefault(-1)
            if (read <= 0) {
                completePlayback(track, opened)
                return
            }
            var offset = 0
            while (offset < read && currentCoroutineContext().isActive) {
                val written = track.write(
                    buffer.array(),
                    offset,
                    read - offset,
                    AudioTrack.WRITE_NON_BLOCKING,
                )
                if (written < 0) return
                offset += written
                if (offset < read) delay(FEED_RETRY_DELAY_MS)
            }
            val position = runCatching { opened.position() }.getOrDefault(0)
            mutableState.update { it.copy(positionMs = bytesToMs(position)) }
        }
    }

    private fun completePlayback(track: AudioTrack, opened: SeekableByteChannel) {
        runCatching { track.stop() }
        runCatching { opened.position(0) }
        mutableState.update { it.copy(isPlaying = false, positionMs = 0) }
    }

    private fun createTrack(): AudioTrack {
        val minBuffer = AudioTrack.getMinBufferSize(
            StreamingAudioRecorder.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        return AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(StreamingAudioRecorder.SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build(),
            maxOf(minBuffer, CHUNK_BYTES * 4),
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
    }

    private fun bytesToMs(bytes: Long): Long = bytes / BYTES_PER_MS

    private fun msToBytes(ms: Long): Long = ms * BYTES_PER_MS

    private companion object {
        const val LOG_TAG = "PcmAudioPlayer"
        const val BYTES_PER_MS = StreamingAudioRecorder.SAMPLE_RATE * 2 / 1000L
        const val CHUNK_BYTES = 3_200
        const val FEED_RETRY_DELAY_MS = 20L
    }
}
