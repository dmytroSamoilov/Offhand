package com.dmytrosamoilov.offhand.core.security

actual fun AudioInputStream.readChunk(buffer: ByteArray): Int = read(buffer)

actual fun AudioInputStream.closeQuietly() {
    runCatching { close() }
}

actual fun AudioOutputStream.writeChunk(bytes: ByteArray, length: Int) = write(bytes, 0, length)

actual fun AudioOutputStream.closeQuietly() {
    runCatching { close() }
}
