package com.dmytrosamoilov.offhand.core.audio

// Decides when a player that polls its position should jump over a pause.
// A player can go on reporting a position from before a jump for a moment
// (the sound still on its way to the speaker or the headphones counts as
// not played yet), and that position is still inside the pause. So after a
// jump or a seek nothing more is decided until playback is seen at or past
// the place it was sent to; otherwise the same jump fires on every reading
// and the audio right after the pause repeats.
class SilenceJumper(private val skips: List<SilenceSkip>) {

    private var settlingUntilMs: Long? = null

    val isEmpty: Boolean get() = skips.isEmpty()

    fun onSeek(positionMs: Long) {
        settlingUntilMs = positionMs
    }

    fun jumpTargetAt(positionMs: Long): Long? {
        if (isSettling(positionMs)) return null
        val skip = skips.firstOrNull { positionMs >= it.startMs && positionMs < it.endMs } ?: return null
        settlingUntilMs = skip.endMs
        return skip.endMs
    }

    fun shownPosition(positionMs: Long): Long = maxOf(positionMs, settlingUntilMs ?: positionMs)

    private fun isSettling(positionMs: Long): Boolean {
        val until = settlingUntilMs ?: return false
        if (positionMs < until) return true
        settlingUntilMs = null
        return false
    }
}
