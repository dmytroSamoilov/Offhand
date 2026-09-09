package com.dmytrosamoilov.offhand.feature.backup.domain.archive

import com.dmytrosamoilov.offhand.core.security.AuthenticationFailedException
import com.dmytrosamoilov.offhand.core.security.BackupCrypto

internal class FakeBackupCrypto : BackupCrypto {

    private var seed = 1

    override fun randomBytes(count: Int): ByteArray = ByteArray(count) { (seed++ and 0xff).toByte() }

    override fun deriveKey(passphrase: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray =
        ByteArray(keyLength) { index ->
            (passphrase.fold(index) { acc, b -> acc * 31 + b } + salt[index % salt.size]).toByte()
        }

    override fun seal(key: ByteArray, nonce: ByteArray, associatedData: ByteArray, plaintext: ByteArray): ByteArray {
        val body = ByteArray(plaintext.size) { i -> (plaintext[i].toInt() xor keystream(key, nonce, i)).toByte() }
        return body + tag(key, nonce, associatedData, plaintext)
    }

    override fun open(key: ByteArray, nonce: ByteArray, associatedData: ByteArray, ciphertext: ByteArray): ByteArray {
        val body = ciphertext.copyOfRange(0, ciphertext.size - TAG_BYTES)
        val plaintext = ByteArray(body.size) { i -> (body[i].toInt() xor keystream(key, nonce, i)).toByte() }
        val expected = tag(key, nonce, associatedData, plaintext)
        if (!expected.contentEquals(ciphertext.copyOfRange(body.size, ciphertext.size))) {
            throw AuthenticationFailedException("tag mismatch")
        }
        return plaintext
    }

    private fun keystream(key: ByteArray, nonce: ByteArray, index: Int): Int =
        (key[index % key.size].toInt() xor nonce[index % nonce.size].toInt() xor (index * 7)) and 0xff

    private fun tag(key: ByteArray, nonce: ByteArray, aad: ByteArray, plaintext: ByteArray): ByteArray {
        var hash = 17L
        (key + nonce + aad + plaintext).forEach { hash = hash * 1_000_003L + it }
        return ByteArray(TAG_BYTES) { i -> (hash ushr ((i % 8) * 8)).toByte() }
    }

    private companion object {
        const val TAG_BYTES = 16
    }
}
