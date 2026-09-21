package com.dmytrosamoilov.offhand.feature.backup.domain.archive

import com.dmytrosamoilov.offhand.core.security.BackupCrypto
import okio.Buffer
import okio.BufferedSink
import okio.Sink
import okio.Timeout

internal class EncryptingSink(
    private val out: BufferedSink,
    private val key: ByteArray,
    private val noncePrefix: ByteArray,
    private val crypto: BackupCrypto,
) : Sink {

    private val pending = Buffer()
    private var counter = 0
    private var closed = false

    override fun write(source: Buffer, byteCount: Long) {
        check(!closed) { "Sink is closed" }
        pending.write(source, byteCount)
        while (pending.size >= BackupFormat.CHUNK_BYTES) {
            emitChunk(final = false)
        }
    }

    override fun flush() = out.flush()

    override fun timeout(): Timeout = out.timeout()

    override fun close() {
        if (closed) return
        closed = true
        while (pending.size > BackupFormat.CHUNK_BYTES) {
            emitChunk(final = false)
        }
        emitChunk(final = true)
        out.close()
    }

    private fun emitChunk(final: Boolean) {
        val plaintext = pending.readByteArray(minOf(pending.size, BackupFormat.CHUNK_BYTES.toLong()))
        val flag = if (final) BackupFormat.FINAL_CHUNK else BackupFormat.MORE_CHUNKS
        val ciphertext = crypto.seal(key, chunkNonce(noncePrefix, counter), chunkAad(counter, flag), plaintext)
        out.writeByte(flag.toInt())
        out.writeInt(ciphertext.size)
        out.write(ciphertext)
        counter += 1
    }
}

internal fun chunkNonce(prefix: ByteArray, counter: Int): ByteArray = ByteArray(BackupFormat.NONCE_BYTES).also { nonce ->
    prefix.copyInto(nonce)
    nonce[8] = (counter ushr 24).toByte()
    nonce[9] = (counter ushr 16).toByte()
    nonce[10] = (counter ushr 8).toByte()
    nonce[11] = counter.toByte()
}

internal fun chunkAad(counter: Int, flag: Byte): ByteArray = BackupFormat.MAGIC + byteArrayOf(
    (counter ushr 24).toByte(),
    (counter ushr 16).toByte(),
    (counter ushr 8).toByte(),
    counter.toByte(),
    flag,
)
