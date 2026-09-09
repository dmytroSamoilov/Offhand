package com.dmytrosamoilov.offhand.core.security

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AndroidBackupCrypto : BackupCrypto {

    private val random = SecureRandom()

    override fun randomBytes(count: Int): ByteArray = ByteArray(count).also(random::nextBytes)

    override fun deriveKey(passphrase: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray =
        Pbkdf2HmacSha256.derive(passphrase, salt, iterations, keyLength)

    override fun seal(key: ByteArray, nonce: ByteArray, associatedData: ByteArray, plaintext: ByteArray): ByteArray =
        cipher(Cipher.ENCRYPT_MODE, key, nonce, associatedData).doFinal(plaintext)

    override fun open(key: ByteArray, nonce: ByteArray, associatedData: ByteArray, ciphertext: ByteArray): ByteArray =
        try {
            cipher(Cipher.DECRYPT_MODE, key, nonce, associatedData).doFinal(ciphertext)
        } catch (failure: AEADBadTagException) {
            throw AuthenticationFailedException(failure.message ?: "Authentication tag mismatch")
        }

    private fun cipher(mode: Int, key: ByteArray, nonce: ByteArray, associatedData: ByteArray): Cipher =
        Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(associatedData)
        }

    private companion object {
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}
