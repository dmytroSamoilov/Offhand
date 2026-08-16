package com.dmytrosamoilov.offhand.feature.notes.domain

import com.dmytrosamoilov.offhand.core.audio.PcmAudioPlayer
import com.dmytrosamoilov.offhand.core.audio.PcmPlaybackState
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

class AndroidAudioPlayer(
    private val player: PcmAudioPlayer,
    private val audioStore: EncryptedAudioStore,
) : AudioPlayer {

    override val state: StateFlow<AudioPlaybackState> = MappedPlaybackStateFlow(player.state)

    override fun load(audioFileName: String) {
        player.load { audioStore.openSeekableForRead(audioFileName) }
    }

    override fun play() = player.play()

    override fun pause() = player.pause()

    override fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    override fun reset() = player.reset()

    override fun release() = player.release()
}

private class MappedPlaybackStateFlow(
    private val source: StateFlow<PcmPlaybackState>,
) : StateFlow<AudioPlaybackState> {

    override val replayCache: List<AudioPlaybackState>
        get() = source.replayCache.map { it.toCommon() }

    override val value: AudioPlaybackState
        get() = source.value.toCommon()

    override suspend fun collect(collector: FlowCollector<AudioPlaybackState>): Nothing =
        source.collect { collector.emit(it.toCommon()) }
}

private fun PcmPlaybackState.toCommon(): AudioPlaybackState = AudioPlaybackState(
    isLoaded = isLoaded,
    isPlaying = isPlaying,
    positionMs = positionMs,
    durationMs = durationMs,
)
