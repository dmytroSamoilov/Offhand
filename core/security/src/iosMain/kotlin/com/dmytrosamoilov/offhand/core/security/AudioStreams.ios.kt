@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.dmytrosamoilov.offhand.core.security

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileHandle
import platform.Foundation.closeFile
import platform.Foundation.create
import platform.Foundation.offsetInFile
import platform.Foundation.readDataOfLength
import platform.Foundation.seekToEndOfFile
import platform.Foundation.seekToFileOffset
import platform.Foundation.writeData
import platform.posix.memcpy

actual class AudioOutputStream internal constructor(
    private val handle: NSFileHandle,
) {

    fun write(bytes: ByteArray) {
        handle.writeData(bytes.toNSData())
    }

    fun close() {
        handle.closeFile()
    }
}

actual class AudioInputStream internal constructor(
    private val handle: NSFileHandle,
) {

    fun read(maxLength: Long): ByteArray? {
        val data = handle.readDataOfLength(maxLength.toULong())
        return if (data.length.toLong() == 0L) null else data.toByteArray()
    }

    fun close() {
        handle.closeFile()
    }
}

actual class AudioSeekableChannel internal constructor(
    private val handle: NSFileHandle,
) {

    fun size(): Long {
        val currentOffset = handle.offsetInFile()
        val endOffset = handle.seekToEndOfFile()
        handle.seekToFileOffset(currentOffset)
        return endOffset.toLong()
    }

    fun close() {
        handle.closeFile()
    }
}

internal fun ByteArray.toNSData(): NSData = if (isEmpty()) {
    NSData()
} else {
    usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
}

internal fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) {
        result.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
    return result
}
