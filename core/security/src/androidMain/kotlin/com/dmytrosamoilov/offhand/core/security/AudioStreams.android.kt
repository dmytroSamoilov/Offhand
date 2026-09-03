package com.dmytrosamoilov.offhand.core.security

import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.SeekableByteChannel

actual class AudioOutputStream(
    private val delegate: OutputStream,
) : OutputStream() {

    override fun write(b: Int) = delegate.write(b)

    override fun write(b: ByteArray) = delegate.write(b)

    override fun write(b: ByteArray, off: Int, len: Int) = delegate.write(b, off, len)

    override fun flush() = delegate.flush()

    override fun close() = delegate.close()
}

actual class AudioInputStream(
    private val delegate: InputStream,
) : InputStream() {

    override fun read(): Int = delegate.read()

    override fun read(b: ByteArray): Int = delegate.read(b)

    override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)

    override fun close() = delegate.close()
}

actual class AudioSeekableChannel(
    private val delegate: SeekableByteChannel,
) : SeekableByteChannel by delegate
