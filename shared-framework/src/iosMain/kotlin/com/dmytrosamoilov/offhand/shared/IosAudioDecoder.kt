@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioDecoder
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioImportException
import com.dmytrosamoilov.offhand.feature.recording.domain.AudioImportLimits
import com.dmytrosamoilov.offhand.feature.recording.domain.DecodedAudio
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.posix.memcpy

// Swift owns AVFoundation: it probes the container and streams 16 kHz mono
// PCM16 chunks back; the return codes stand in for exceptions, which cannot
// cross the closure boundary.
interface IosAudioDecoderBridge {

    fun probe(path: String): Long

    fun decode(path: String, onPcm: (NSData) -> Unit, onProgress: (Float) -> Unit): Boolean

    companion object {
        const val UNSUPPORTED = -1L
        const val UNREADABLE = -2L
    }
}

class IosAudioDecoder(private val bridge: IosAudioDecoderBridge) : AudioDecoder {

    override suspend fun decode(
        source: AudioImportSource,
        onPcm: (bytes: ByteArray, length: Int) -> Unit,
        onProgress: (Float) -> Unit,
    ): DecodedAudio = withContext(Dispatchers.IO) {
        val durationMs = probeOrThrow(source.handle)
        val decoded = bridge.decode(
            path = source.handle,
            onPcm = { data -> data.toBytes().let { bytes -> onPcm(bytes, bytes.size) } },
            onProgress = onProgress,
        )
        if (!decoded) throw AudioImportException.Unreadable(null)
        DecodedAudio(durationMs)
    }

    private fun probeOrThrow(path: String): Long {
        val durationMs = bridge.probe(path)
        return when {
            durationMs == IosAudioDecoderBridge.UNSUPPORTED -> throw AudioImportException.Unsupported()
            durationMs < 0 -> throw AudioImportException.Unreadable(null)
            durationMs > AudioImportLimits.MAX_DURATION_MS -> throw AudioImportException.TooLong(durationMs)
            else -> durationMs
        }
    }

    override fun discard(source: AudioImportSource) {
        NSFileManager.defaultManager.removeItemAtPath(source.handle, null)
    }
}

private fun NSData.toBytes(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) {
        result.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
    return result
}
