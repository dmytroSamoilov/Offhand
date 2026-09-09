package com.dmytrosamoilov.offhand.feature.backup.domain.archive

import com.dmytrosamoilov.offhand.feature.backup.domain.BackupException
import okio.BufferedSink
import okio.BufferedSource

internal data class RecordHeader(
    val kind: RecordKind,
    val name: String,
    val length: Long,
)

internal fun BufferedSink.writeRecordHeader(kind: RecordKind, name: String, length: Long) {
    val nameBytes = name.encodeToByteArray()
    writeByte(kind.code.toInt())
    writeShort(nameBytes.size)
    write(nameBytes)
    writeLong(length)
}

internal fun BufferedSink.writeTextRecord(kind: RecordKind, text: String) {
    val bytes = text.encodeToByteArray()
    writeRecordHeader(kind, name = "", length = bytes.size.toLong())
    write(bytes)
}

internal fun BufferedSink.writeEndRecord() = writeRecordHeader(RecordKind.END, name = "", length = 0L)

internal fun BufferedSource.readRecordHeader(): RecordHeader {
    val kind = RecordKind.fromCode(readByte()) ?: throw BackupException.Corrupt("Unknown record type")
    val nameLength = readShort().toInt()
    if (nameLength < 0) throw BackupException.Corrupt("Invalid record name")
    val name = readByteArray(nameLength.toLong()).decodeToString()
    val length = readLong()
    if (length < 0) throw BackupException.Corrupt("Invalid record length")
    return RecordHeader(kind, name, length)
}

internal fun BufferedSource.readTextRecord(header: RecordHeader): String =
    readByteArray(header.length).decodeToString()
