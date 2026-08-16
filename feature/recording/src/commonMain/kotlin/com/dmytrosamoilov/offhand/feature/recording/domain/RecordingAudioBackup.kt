package com.dmytrosamoilov.offhand.feature.recording.domain

interface RecordingAudioBackup {

    fun openWrite(fileName: String): Boolean

    fun write(pcm: ByteArray): Boolean

    fun closeWrite()

    fun openRead(fileName: String): Boolean

    fun readChunk(buffer: ByteArray, offset: Int, length: Int): Int

    fun closeRead()
}
