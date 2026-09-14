package com.dmytrosamoilov.offhand.core.security

actual fun AudioInputStream.readChunk(buffer: ByteArray): Int {
    val chunk = read(buffer.size.toLong()) ?: return -1
    chunk.copyInto(buffer)
    return chunk.size
}

actual fun AudioInputStream.closeQuietly() {
    runCatching { close() }
}

actual fun AudioOutputStream.writeChunk(bytes: ByteArray, length: Int) {
    write(if (length == bytes.size) bytes else bytes.copyOf(length))
}

actual fun AudioOutputStream.closeQuietly() {
    runCatching { close() }
}
