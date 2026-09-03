package com.dmytrosamoilov.offhand.core.audio

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioChunkerTest {

    private fun chunker() = AudioChunker(
        sampleRate = SAMPLE_RATE,
        minChunkMs = 1_000L,
        maxChunkMs = 3_000L,
        silenceGapMs = 200L,
        silenceDb = -45f,
    )

    private val loudFrame = ShortArray(FRAME_SAMPLES) { 8_000 }
    private val silentFrame = ShortArray(FRAME_SAMPLES)

    private fun AudioChunker.feed(frame: ShortArray, times: Int): AudioChunker.PcmChunk? {
        var lastChunk: AudioChunker.PcmChunk? = null
        repeat(times) {
            onFrame(frame, frame.size, isPaused = false).completedChunk?.let { lastChunk = it }
        }
        return lastChunk
    }

    @Test
    fun maxDurationProducesBoundary() {
        val chunk = chunker().feed(loudFrame, times = 60)
        checkNotNull(chunk)
        assertEquals(ChunkBoundaryReason.MAX_DURATION, chunk.reason)
        assertEquals(1, chunk.id)
        assertEquals(3_000L, chunk.durationMs)
        assertEquals(3_000L, chunk.speechMs)
        assertEquals(60 * FRAME_SAMPLES * 2, chunk.pcm.size)
    }

    @Test
    fun silenceGapAfterMinChunkProducesBoundary() {
        val chunker = chunker()
        chunker.feed(loudFrame, times = 20)
        val chunk = chunker.feed(silentFrame, times = 4)
        checkNotNull(chunk)
        assertEquals(ChunkBoundaryReason.SILENCE_GAP, chunk.reason)
        assertEquals(1_200L, chunk.durationMs)
        assertEquals(1_000L, chunk.speechMs)
    }

    @Test
    fun silenceBeforeMinChunkProducesNoBoundary() {
        val chunker = chunker()
        chunker.feed(loudFrame, times = 10)
        assertNull(chunker.feed(silentFrame, times = 4))
    }

    @Test
    fun pausedFramesAreDropped() {
        val chunker = chunker()
        chunker.feed(loudFrame, times = 2)
        repeat(3) {
            val outcome = chunker.onFrame(loudFrame, loudFrame.size, isPaused = true)
            assertNull(outcome.frameBytes)
            assertNull(outcome.completedChunk)
            assertTrue(outcome.vad.isPaused)
            assertEquals(100L, outcome.vad.chunkElapsedMs)
        }
        val tail = checkNotNull(chunker.drainTail())
        assertEquals(100L, tail.durationMs)
        assertEquals(2 * FRAME_SAMPLES * 2, tail.pcm.size)
    }

    @Test
    fun drainTailProducesUserStopChunk() {
        val chunker = chunker()
        chunker.feed(loudFrame, times = 5)
        val tail = checkNotNull(chunker.drainTail())
        assertEquals(ChunkBoundaryReason.USER_STOP, tail.reason)
        assertEquals(1, tail.id)
        assertEquals(250L, tail.durationMs)
        assertEquals(250L, tail.speechMs)
        assertNull(chunker.drainTail())
    }

    @Test
    fun framesAreEncodedLittleEndian() {
        val chunker = chunker()
        val frame = ShortArray(1) { 0x1234 }
        val outcome = chunker.onFrame(frame, 1, isPaused = false)
        assertContentEquals(byteArrayOf(0x34, 0x12), outcome.frameBytes)
    }

    @Test
    fun chunkIdAdvancesAfterBoundary() {
        val chunker = chunker()
        chunker.feed(loudFrame, times = 60)
        val outcome = chunker.onFrame(loudFrame, loudFrame.size, isPaused = false)
        assertEquals(2, outcome.vad.currentChunkId)
        assertEquals(50L, outcome.vad.chunkElapsedMs)
        assertEquals(3_050L, outcome.vad.totalElapsedMs)
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val FRAME_SAMPLES = 800
    }
}
