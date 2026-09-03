package com.dmytrosamoilov.offhand.feature.recording.domain

import co.touchlab.kermit.Logger
import com.dmytrosamoilov.offhand.core.security.AudioInputStream
import com.dmytrosamoilov.offhand.core.security.AudioOutputStream
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore

class IosRecordingAudioBackup(
    private val audioStore: EncryptedAudioStore,
) : RecordingAudioBackup {

    private var writeStream: AudioOutputStream? = null
    private var readStream: AudioInputStream? = null
    private var readBuffer: ByteArray = ByteArray(0)
    private var readBufferPosition = 0

    override fun openWrite(fileName: String): Boolean = try {
        writeStream = audioStore.openForWrite(fileName)
        true
    } catch (t: Throwable) {
        Logger.withTag(LOG_TAG).w(t) { "Audio backup unavailable, recording without it" }
        writeStream = null
        false
    }

    override fun write(pcm: ByteArray): Boolean {
        val stream = writeStream ?: return false
        return try {
            stream.write(pcm)
            true
        } catch (t: Throwable) {
            Logger.withTag(LOG_TAG).w(t) { "Audio backup write failed, dropping the file" }
            runCatching { stream.close() }
            writeStream = null
            false
        }
    }

    override fun closeWrite() {
        writeStream?.let { stream ->
            runCatching { stream.close() }
                .onFailure { Logger.withTag(LOG_TAG).w(it) { "Audio backup close failed" } }
        }
        writeStream = null
    }

    override fun openRead(fileName: String): Boolean = try {
        readStream = audioStore.openForRead(fileName)
        readBuffer = ByteArray(0)
        readBufferPosition = 0
        true
    } catch (t: Throwable) {
        Logger.withTag(LOG_TAG).w(t) { "Stored audio unavailable for reading" }
        readStream = null
        false
    }

    override fun readChunk(buffer: ByteArray, offset: Int, length: Int): Int {
        val stream = readStream ?: return -1
        return try {
            if (readBufferPosition >= readBuffer.size) {
                readBuffer = stream.read(length.toLong()) ?: return -1
                readBufferPosition = 0
            }
            val count = minOf(length, readBuffer.size - readBufferPosition)
            readBuffer.copyInto(buffer, offset, readBufferPosition, readBufferPosition + count)
            readBufferPosition += count
            count
        } catch (t: Throwable) {
            Logger.withTag(LOG_TAG).w(t) { "Stored audio ends early, keeping the readable part" }
            -1
        }
    }

    override fun closeRead() {
        runCatching { readStream?.close() }
        readStream = null
        readBuffer = ByteArray(0)
        readBufferPosition = 0
    }

    private companion object {
        const val LOG_TAG = "RecordingSession"
    }
}
