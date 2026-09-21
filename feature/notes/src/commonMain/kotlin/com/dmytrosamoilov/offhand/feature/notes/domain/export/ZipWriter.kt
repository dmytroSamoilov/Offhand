package com.dmytrosamoilov.offhand.feature.notes.domain.export

// A stored (uncompressed) ZIP is enough for DOCX and keeps the writer free of
// platform deflate APIs; Word and Pages open it like any other package.
class ZipWriter {

    private class Entry(val name: ByteArray, val data: ByteArray, val crc: Int, val offset: Int)

    private val entries = mutableListOf<Entry>()
    private val body = ByteArrayBuilder()

    fun add(name: String, data: ByteArray) {
        val nameBytes = name.encodeToByteArray()
        val crc = Crc32.of(data)
        entries += Entry(nameBytes, data, crc, body.size)
        body.int(LOCAL_HEADER).short(VERSION).short(FLAGS).short(STORED).int(DOS_TIME)
            .int(crc).int(data.size).int(data.size).short(nameBytes.size).short(0)
            .bytes(nameBytes).bytes(data)
    }

    fun toByteArray(): ByteArray {
        val directoryOffset = body.size
        val directory = ByteArrayBuilder()
        entries.forEach { entry ->
            directory.int(CENTRAL_HEADER).short(VERSION).short(VERSION).short(FLAGS).short(STORED).int(DOS_TIME)
                .int(entry.crc).int(entry.data.size).int(entry.data.size).short(entry.name.size)
                .short(0).short(0).short(0).short(0).int(0).int(entry.offset).bytes(entry.name)
        }
        val directorySize = directory.size
        directory.int(END_OF_DIRECTORY).short(0).short(0).short(entries.size).short(entries.size)
            .int(directorySize).int(directoryOffset).short(0)
        return body.toByteArray() + directory.toByteArray()
    }

    private class ByteArrayBuilder {
        private var buffer = ByteArray(INITIAL_CAPACITY)
        var size = 0
            private set

        fun bytes(value: ByteArray): ByteArrayBuilder {
            ensure(value.size)
            value.copyInto(buffer, size)
            size += value.size
            return this
        }

        fun short(value: Int): ByteArrayBuilder = bytes(byteArrayOf(value.toByte(), (value ushr 8).toByte()))

        fun int(value: Int): ByteArrayBuilder = bytes(
            byteArrayOf(value.toByte(), (value ushr 8).toByte(), (value ushr 16).toByte(), (value ushr 24).toByte()),
        )

        fun toByteArray(): ByteArray = buffer.copyOf(size)

        private fun ensure(extra: Int) {
            if (size + extra > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, size + extra))
        }
    }

    private companion object {
        const val LOCAL_HEADER = 0x04034b50
        const val CENTRAL_HEADER = 0x02014b50
        const val END_OF_DIRECTORY = 0x06054b50
        const val VERSION = 20
        const val FLAGS = 0x0800
        const val STORED = 0
        const val DOS_TIME = 0x5A210000
        const val INITIAL_CAPACITY = 4096
    }
}

object Crc32 {

    private val table = IntArray(256) { index ->
        var crc = index
        repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor POLYNOMIAL else crc ushr 1 }
        crc
    }

    fun of(data: ByteArray): Int {
        var crc = -1
        data.forEach { byte -> crc = table[(crc xor byte.toInt()) and 0xFF] xor (crc ushr 8) }
        return crc.inv()
    }

    private const val POLYNOMIAL = 0xEDB88320.toInt()
}
