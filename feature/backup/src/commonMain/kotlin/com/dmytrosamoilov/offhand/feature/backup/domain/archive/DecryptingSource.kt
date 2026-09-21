package com.dmytrosamoilov.offhand.feature.backup.domain.archive

import com.dmytrosamoilov.offhand.core.security.AuthenticationFailedException
import com.dmytrosamoilov.offhand.core.security.BackupCrypto
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupException
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout

internal class DecryptingSource(
    private val input: BufferedSource,
    private val key: ByteArray,
    private val noncePrefix: ByteArray,
    private val crypto: BackupCrypto,
) : Source {

    private val plaintext = Buffer()
    private var counter = 0
    private var finished = false

    override fun read(sink: Buffer, byteCount: Long): Long {
        while (plaintext.size == 0L && !finished) {
            readChunk()
        }
        if (plaintext.size == 0L) return -1L
        return plaintext.read(sink, byteCount)
    }

    override fun timeout(): Timeout = input.timeout()

    override fun close() = input.close()

    private fun readChunk() {
        if (input.exhausted()) throw BackupException.Corrupt("Backup ends before its final chunk")
        val flag = input.readByte()
        val length = input.readInt()
        if (flag != BackupFormat.FINAL_CHUNK && flag != BackupFormat.MORE_CHUNKS) {
            throw BackupException.Corrupt("Unexpected chunk flag")
        }
        if (length < BackupFormat.TAG_BYTES || length > BackupFormat.MAX_CIPHERTEXT_BYTES) {
            throw BackupException.Corrupt("Chunk length out of range")
        }
        if (input.request(length.toLong()).not()) throw BackupException.Corrupt("Chunk is truncated")
        val ciphertext = input.readByteArray(length.toLong())
        val decrypted = try {
            crypto.open(key, chunkNonce(noncePrefix, counter), chunkAad(counter, flag), ciphertext)
        } catch (failure: AuthenticationFailedException) {
            if (counter == 0) throw BackupException.WrongPassphrase()
            throw BackupException.Corrupt("Chunk $counter failed authentication")
        }
        plaintext.write(decrypted)
        counter += 1
        finished = flag == BackupFormat.FINAL_CHUNK
    }
}
