package com.dmytrosamoilov.offhand.feature.backup.domain.archive

internal object BackupFormat {
    val MAGIC: ByteArray = "OFHB".encodeToByteArray()
    const val VERSION = 2
    const val MIN_SUPPORTED_VERSION = 1
    const val SALT_BYTES = 16
    const val NONCE_PREFIX_BYTES = 8
    const val NONCE_BYTES = 12
    const val KEY_BYTES = 32
    const val TAG_BYTES = 16
    const val KDF_ITERATIONS = 600_000
    const val CHUNK_BYTES = 1 shl 20
    const val MAX_CIPHERTEXT_BYTES = CHUNK_BYTES + TAG_BYTES
    const val FINAL_CHUNK: Byte = 1
    const val MORE_CHUNKS: Byte = 0
}

internal enum class RecordKind(val code: Byte) {
    END(0),
    MANIFEST(1),
    FOLDERS(2),
    NOTES(3),
    AUDIO(4),
    STYLES(5);

    companion object {
        fun fromCode(code: Byte): RecordKind? = entries.firstOrNull { it.code == code }
    }
}
