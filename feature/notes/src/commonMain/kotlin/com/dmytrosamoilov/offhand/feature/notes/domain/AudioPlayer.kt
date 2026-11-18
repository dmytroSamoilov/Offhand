package com.dmytrosamoilov.offhand.feature.notes.domain

import com.dmytrosamoilov.offhand.core.audio.SilenceSkip
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {

    val state: StateFlow<AudioPlaybackState>

    fun load(audioFileName: String)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    // Stretches playback jumps over; loading a recording clears them.
    fun setSilenceSkips(skips: List<SilenceSkip>)

    fun reset()

    fun release()
}

data class AudioPlaybackState(
    val isLoaded: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
)
