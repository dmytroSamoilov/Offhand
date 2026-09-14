package com.dmytrosamoilov.offhand.core.security

expect fun AudioInputStream.readChunk(buffer: ByteArray): Int

expect fun AudioInputStream.closeQuietly()

expect fun AudioOutputStream.writeChunk(bytes: ByteArray, length: Int)

expect fun AudioOutputStream.closeQuietly()
