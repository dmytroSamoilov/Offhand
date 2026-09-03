package com.dmytrosamoilov.offhand.feature.recording.domain

import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import timber.log.Timber

class AndroidRecordingAudioBackup(
    private val audioStore: EncryptedAudioStore,
) : RecordingAudioBackup {

    private var writeStream: OutputStream? = null
    private var readStream: InputStream? = null

    override fun openWrite(fileName: String): Boolean = try {
        writeStream = BufferedOutputStream(audioStore.openForWrite(fileName))
        true
    } catch (t: Throwable) {
        Timber.tag(LOG_TAG).w(t, "Audio backup unavailable, recording without it")
        writeStream = null
        false
    }

    override fun write(pcm: ByteArray): Boolean {
        val stream = writeStream ?: return false
        return try {
            stream.write(pcm)
            true
        } catch (io: IOException) {
            Timber.tag(LOG_TAG).w(io, "Audio backup write failed, dropping the file")
            runCatching { stream.close() }
            writeStream = null
            false
        }
    }

    override fun closeWrite() {
        writeStream?.let { stream ->
            runCatching { stream.close() }
                .onFailure { Timber.tag(LOG_TAG).w(it, "Audio backup close failed") }
        }
        writeStream = null
    }

    override fun openRead(fileName: String): Boolean = try {
        readStream = audioStore.openForRead(fileName)
        true
    } catch (t: Throwable) {
        Timber.tag(LOG_TAG).w(t, "Stored audio unavailable for reading")
        readStream = null
        false
    }

    override fun readChunk(buffer: ByteArray, offset: Int, length: Int): Int {
        val stream = readStream ?: return -1
        return try {
            stream.read(buffer, offset, length)
        } catch (io: IOException) {
            Timber.tag(LOG_TAG).w(io, "Stored audio ends early, keeping the readable part")
            -1
        }
    }

    override fun closeRead() {
        runCatching { readStream?.close() }
        readStream = null
    }

    private companion object {
        const val LOG_TAG = "RecordingSession"
    }
}
