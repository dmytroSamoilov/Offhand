package com.dmytrosamoilov.offhand.core.audio

class RoomSilenceThreshold(private val quietRoomDb: Float) {

    private class Level(val db: Float, val durationMs: Long)

    private val levels = ArrayDeque<Level>()
    private var windowMs = 0L

    var currentDb: Float = quietRoomDb
        private set

    fun onFrame(rmsDb: Float, frameMs: Long) {
        levels.addLast(Level(rmsDb, frameMs))
        windowMs += frameMs
        while (windowMs > WINDOW_MS && levels.size > 1) windowMs -= levels.removeFirst().durationMs
        currentDb = (noiseFloorDb() + MARGIN_DB).coerceIn(quietRoomDb, maxOf(quietRoomDb, NOISIEST_ROOM_DB))
    }

    private fun noiseFloorDb(): Float {
        val sorted = levels.map { it.db }.sorted()
        return sorted[(sorted.size * NOISE_FLOOR_PERCENTILE).toInt().coerceAtMost(sorted.lastIndex)]
    }

    // The quietest tenth of the last seconds is the room itself; anything
    // within the margin above it is the room too, not somebody speaking.
    companion object {
        const val WINDOW_MS = 10_000L
        const val MARGIN_DB = 8f
        const val NOISIEST_ROOM_DB = -33f
        private const val NOISE_FLOOR_PERCENTILE = 0.1
    }
}
