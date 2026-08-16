@file:OptIn(ExperimentalForeignApi::class)

package com.dmytrosamoilov.offhand.core.security

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileProtectionCompleteUnlessOpen
import platform.Foundation.NSFileProtectionKey
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.closeFile
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.fileHandleForWritingAtPath
import platform.Foundation.seekToEndOfFile
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.truncateFileAtOffset

class IosEncryptedAudioStore : EncryptedAudioStore {

    private val fileManager: NSFileManager get() = NSFileManager.defaultManager

    private val recordingsDirectory: String
        get() = "${documentsDirectory()}/$RECORDINGS_DIR".also { path ->
            if (!fileManager.fileExistsAtPath(path)) {
                fileManager.createDirectoryAtPath(
                    path,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null,
                )
            }
        }

    override fun newRecordingFileName(): String = "note-${NSUUID().UUIDString}$FILE_EXTENSION"

    override fun openForWrite(fileName: String): AudioOutputStream {
        val path = pathFor(fileName)
        if (!fileManager.fileExistsAtPath(path)) {
            fileManager.createFileAtPath(
                path,
                contents = null,
                attributes = mapOf(NSFileProtectionKey to NSFileProtectionCompleteUnlessOpen),
            )
        }
        val handle = requireNotNull(NSFileHandle.fileHandleForWritingAtPath(path)) {
            "Cannot open $fileName for writing"
        }
        handle.truncateFileAtOffset(0uL)
        return AudioOutputStream(handle)
    }

    override fun openForRead(fileName: String): AudioInputStream =
        AudioInputStream(openHandleForReading(fileName))

    override fun openSeekableForRead(fileName: String): AudioSeekableChannel =
        AudioSeekableChannel(openHandleForReading(fileName))

    private fun openHandleForReading(fileName: String): NSFileHandle =
        requireNotNull(NSFileHandle.fileHandleForReadingAtPath(pathFor(fileName))) {
            "Cannot open $fileName for reading"
        }

    override fun delete(fileName: String) {
        fileManager.removeItemAtPath(pathFor(fileName), error = null)
    }

    override fun sizeOf(fileName: String): Long {
        val path = pathFor(fileName)
        if (!fileManager.fileExistsAtPath(path)) return 0L
        val handle = NSFileHandle.fileHandleForReadingAtPath(path) ?: return 0L
        return handle.seekToEndOfFile().toLong().also { handle.closeFile() }
    }

    // iOS has no ciphertext framing to account for: Data Protection encrypts the file at
    // rest, so the file's byte size already equals the raw PCM size.
    override fun pcmSizeOf(fileName: String): Long = sizeOf(fileName)

    override fun deleteUnreferenced(referencedFileNames: Set<String>, minAgeMs: Long): Int {
        val cutoffEpochSeconds = NSDate().timeIntervalSince1970 - (minAgeMs / MS_PER_SECOND)
        val candidateNames = fileManager.contentsOfDirectoryAtPath(recordingsDirectory, error = null)
            .orEmpty()
            .map { it as String }
            .filterNot { it in referencedFileNames }
        return candidateNames.count { deleteIfStale(it, cutoffEpochSeconds) }
    }

    private fun deleteIfStale(fileName: String, cutoffEpochSeconds: Double): Boolean {
        val path = "$recordingsDirectory/$fileName"
        val attributes = fileManager.attributesOfItemAtPath(path, error = null) ?: return false
        val modifiedAtSeconds = (attributes[NSFileModificationDate] as? NSDate)?.timeIntervalSince1970
            ?: return false
        return modifiedAtSeconds < cutoffEpochSeconds && fileManager.removeItemAtPath(path, error = null)
    }

    private fun pathFor(fileName: String): String {
        require('/' !in fileName && ".." !in fileName) {
            "Recording file name escapes the recordings directory"
        }
        return "$recordingsDirectory/$fileName"
    }

    private companion object {
        const val RECORDINGS_DIR = "recordings"
        const val FILE_EXTENSION = ".pcm.enc"
        const val MS_PER_SECOND = 1000.0
    }
}

internal fun documentsDirectory(): String =
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
