package com.dmytrosamoilov.offhand.core.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal object Pbkdf2HmacSha256 {

    fun derive(passphrase: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        require(iterations > 0 && keyLength > 0) { "Invalid key derivation parameters" }
        val mac = Mac.getInstance(ALGORITHM).apply { init(SecretKeySpec(passphrase, ALGORITHM)) }
        val output = ByteArray(keyLength)
        var produced = 0
        var blockIndex = 1
        while (produced < keyLength) {
            val block = deriveBlock(mac, salt, iterations, blockIndex)
            val count = minOf(block.size, keyLength - produced)
            block.copyInto(output, produced, 0, count)
            produced += count
            blockIndex += 1
        }
        return output
    }

    private fun deriveBlock(mac: Mac, salt: ByteArray, iterations: Int, blockIndex: Int): ByteArray {
        mac.update(salt)
        var previous = mac.doFinal(blockIndexBytes(blockIndex))
        val block = previous.copyOf()
        repeat(iterations - 1) {
            previous = mac.doFinal(previous)
            for (i in block.indices) block[i] = (block[i].toInt() xor previous[i].toInt()).toByte()
        }
        return block
    }

    private fun blockIndexBytes(index: Int): ByteArray = byteArrayOf(
        (index ushr 24).toByte(),
        (index ushr 16).toByte(),
        (index ushr 8).toByte(),
        index.toByte(),
    )

    private const val ALGORITHM = "HmacSHA256"
}
