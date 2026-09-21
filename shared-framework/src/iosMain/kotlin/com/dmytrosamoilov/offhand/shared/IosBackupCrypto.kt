@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.dmytrosamoilov.offhand.shared

import com.dmytrosamoilov.offhand.core.security.AuthenticationFailedException
import com.dmytrosamoilov.offhand.core.security.BackupCrypto
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy

interface IosBackupCryptoBridge {

    fun randomBytes(count: Int): NSData

    fun deriveKey(passphrase: NSData, salt: NSData, iterations: Int, keyLength: Int): NSData

    fun seal(key: NSData, nonce: NSData, associatedData: NSData, plaintext: NSData): NSData

    fun open(key: NSData, nonce: NSData, associatedData: NSData, ciphertext: NSData): NSData?
}

class IosBackupCrypto(private val bridge: IosBackupCryptoBridge) : BackupCrypto {

    override fun randomBytes(count: Int): ByteArray = bridge.randomBytes(count).toBytes()

    override fun deriveKey(passphrase: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray =
        bridge.deriveKey(passphrase.toData(), salt.toData(), iterations, keyLength).toBytes()

    override fun seal(key: ByteArray, nonce: ByteArray, associatedData: ByteArray, plaintext: ByteArray): ByteArray =
        bridge.seal(key.toData(), nonce.toData(), associatedData.toData(), plaintext.toData()).toBytes()

    override fun open(key: ByteArray, nonce: ByteArray, associatedData: ByteArray, ciphertext: ByteArray): ByteArray =
        bridge.open(key.toData(), nonce.toData(), associatedData.toData(), ciphertext.toData())?.toBytes()
            ?: throw AuthenticationFailedException("Authentication tag mismatch")
}

private fun ByteArray.toData(): NSData = if (isEmpty()) {
    NSData()
} else {
    usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
}

private fun NSData.toBytes(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) {
        result.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
    return result
}
