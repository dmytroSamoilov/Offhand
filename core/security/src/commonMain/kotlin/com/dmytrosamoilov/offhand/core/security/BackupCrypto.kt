package com.dmytrosamoilov.offhand.core.security

interface BackupCrypto {

    fun randomBytes(count: Int): ByteArray

    fun deriveKey(passphrase: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray

    fun seal(key: ByteArray, nonce: ByteArray, associatedData: ByteArray, plaintext: ByteArray): ByteArray

    fun open(key: ByteArray, nonce: ByteArray, associatedData: ByteArray, ciphertext: ByteArray): ByteArray
}

class AuthenticationFailedException(message: String) : Exception(message)
