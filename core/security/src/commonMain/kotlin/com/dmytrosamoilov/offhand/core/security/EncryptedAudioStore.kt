package com.dmytrosamoilov.offhand.core.security

interface EncryptedAudioStore {

    fun newRecordingFileName(): String

    fun openForWrite(fileName: String): AudioOutputStream

    fun openForRead(fileName: String): AudioInputStream

    fun openSeekableForRead(fileName: String): AudioSeekableChannel

    fun delete(fileName: String)

    fun sizeOf(fileName: String): Long

    fun pcmSizeOf(fileName: String): Long

    fun deleteUnreferenced(referencedFileNames: Set<String>, minAgeMs: Long): Int
}
