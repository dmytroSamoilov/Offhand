package com.dmytrosamoilov.offhand.feature.backup.domain.archive

import com.dmytrosamoilov.offhand.core.security.BackupCrypto
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupException
import okio.BufferedSink
import okio.BufferedSource
import okio.Sink
import okio.Source
import okio.buffer

internal class BackupArchive(private val crypto: BackupCrypto) {

    fun openForWrite(sink: Sink, passphrase: ByteArray): BufferedSink {
        val out = sink.buffer()
        val salt = crypto.randomBytes(BackupFormat.SALT_BYTES)
        val noncePrefix = crypto.randomBytes(BackupFormat.NONCE_PREFIX_BYTES)
        out.write(BackupFormat.MAGIC)
        out.writeByte(BackupFormat.VERSION)
        out.writeInt(BackupFormat.KDF_ITERATIONS)
        out.write(salt)
        out.write(noncePrefix)
        val key = crypto.deriveKey(passphrase, salt, BackupFormat.KDF_ITERATIONS, BackupFormat.KEY_BYTES)
        return EncryptingSink(out, key, noncePrefix, crypto).buffer()
    }

    fun openForRead(source: Source, passphrase: ByteArray): BufferedSource {
        val input = source.buffer()
        val headerBytes = BackupFormat.MAGIC.size + 1 + Int.SIZE_BYTES + BackupFormat.SALT_BYTES + BackupFormat.NONCE_PREFIX_BYTES
        if (!input.request(headerBytes.toLong())) throw BackupException.Corrupt("File is too short to be a backup")
        val magic = input.readByteArray(BackupFormat.MAGIC.size.toLong())
        if (!magic.contentEquals(BackupFormat.MAGIC)) throw BackupException.Corrupt("Not an Offhand backup")
        val version = input.readByte().toInt()
        if (version !in BackupFormat.MIN_SUPPORTED_VERSION..BackupFormat.VERSION) {
            throw BackupException.UnsupportedVersion(version)
        }
        val iterations = input.readInt()
        if (iterations <= 0) throw BackupException.Corrupt("Invalid key derivation parameters")
        val salt = input.readByteArray(BackupFormat.SALT_BYTES.toLong())
        val noncePrefix = input.readByteArray(BackupFormat.NONCE_PREFIX_BYTES.toLong())
        val key = crypto.deriveKey(passphrase, salt, iterations, BackupFormat.KEY_BYTES)
        return DecryptingSource(input, key, noncePrefix, crypto).buffer()
    }
}
