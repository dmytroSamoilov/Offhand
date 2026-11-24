package com.dmytrosamoilov.offhand.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals

class RoomSilenceThresholdTest {

    private fun RoomSilenceThreshold.feed(db: Float, times: Int) = repeat(times) { onFrame(db, FRAME_MS) }

    @Test
    fun aQuietRoomKeepsTheQuietRoomThreshold() {
        val threshold = RoomSilenceThreshold(QUIET_ROOM_DB)
        threshold.feed(-60f, times = 40)
        threshold.feed(-20f, times = 40)
        assertEquals(QUIET_ROOM_DB, threshold.currentDb)
    }

    @Test
    fun aNoisyRoomRaisesTheThresholdAboveItsNoise() {
        val threshold = RoomSilenceThreshold(QUIET_ROOM_DB)
        threshold.feed(-41f, times = 40)
        threshold.feed(-20f, times = 40)
        assertEquals(-41f + RoomSilenceThreshold.MARGIN_DB, threshold.currentDb)
    }

    @Test
    fun theThresholdNeverPassesTheNoisiestRoom() {
        val threshold = RoomSilenceThreshold(QUIET_ROOM_DB)
        threshold.feed(-20f, times = 40)
        assertEquals(RoomSilenceThreshold.NOISIEST_ROOM_DB, threshold.currentDb)
    }

    @Test
    fun noiseOlderThanTheWindowIsForgotten() {
        val threshold = RoomSilenceThreshold(QUIET_ROOM_DB)
        threshold.feed(-41f, times = 40)
        threshold.feed(-60f, times = (RoomSilenceThreshold.WINDOW_MS / FRAME_MS).toInt())
        assertEquals(QUIET_ROOM_DB, threshold.currentDb)
    }

    private companion object {
        const val QUIET_ROOM_DB = -45f
        const val FRAME_MS = 50L
    }
}
