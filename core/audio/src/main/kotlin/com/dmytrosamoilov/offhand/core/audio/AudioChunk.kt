package com.dmytrosamoilov.offhand.core.audio

data class AudioChunk(
    val id: Int,
    val wav: ByteArray,
    val durationMs: Long,
    val reason: ChunkBoundaryReason,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioChunk
        if (id != other.id) return false
        if (durationMs != other.durationMs) return false
        if (!wav.contentEquals(other.wav)) return false
        if (reason != other.reason) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + wav.contentHashCode()
        result = 31 * result + reason.hashCode()
        return result
    }
}

enum class ChunkBoundaryReason {
    SILENCE_GAP,
    MAX_DURATION,
    USER_STOP,
}

data class VadSnapshot(
    val rmsDb: Float = SILENCE_DB,
    val isSilent: Boolean = true,
    val isPaused: Boolean = false,
    val chunkElapsedMs: Long = 0L,
    val totalElapsedMs: Long = 0L,
    val currentChunkId: Int = 0,
) {
    companion object {
        const val SILENCE_DB = -120f
    }
}
