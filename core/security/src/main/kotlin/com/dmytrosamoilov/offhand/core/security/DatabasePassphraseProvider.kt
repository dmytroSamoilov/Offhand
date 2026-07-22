package com.dmytrosamoilov.offhand.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

class PassphraseInvalidatedException(cause: Throwable) : Exception(
    "Keystore key was permanently invalidated; the wrapped passphrase is unrecoverable.",
    cause,
)

@Singleton
class DatabasePassphraseProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @Volatile
    private var cached: ByteArray? = null

    @Synchronized
    fun passphrase(): ByteArray {
        cached?.let { return it }
        val wrappedFile = File(context.filesDir, WRAPPED_PASSPHRASE_FILE)
        val key = getOrCreateKey()
        val plain = if (wrappedFile.exists()) {
            unwrap(wrappedFile, key).also { migrateAuthBoundKey(key, it, wrappedFile) }
        } else {
            createAndWrap(wrappedFile, key)
        }
        cached = plain
        return plain
    }

    fun warmUp() {
        passphrase()
    }

    @Synchronized
    fun reset() {
        cached = null
        File(context.filesDir, WRAPPED_PASSPHRASE_FILE).delete()
        loadKeyStore().deleteEntry(KEY_ALIAS)
        Timber.tag(LOG_TAG).w("Encrypted storage key reset")
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = loadKeyStore()
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    // Keys created before 0.9.1 were auth-bound with a 60 s validity window, which crashes
    // any database access happening outside that window (onboarding, background services).
    private fun migrateAuthBoundKey(key: SecretKey, plain: ByteArray, wrappedFile: File) {
        if (!isAuthBound(key)) return
        loadKeyStore().deleteEntry(KEY_ALIAS)
        wrap(plain, getOrCreateKey(), wrappedFile)
        Timber.tag(LOG_TAG).i("Re-wrapped database passphrase with a non-auth-bound key")
    }

    private fun isAuthBound(key: SecretKey): Boolean = runCatching {
        val factory = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE)
        (factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo).isUserAuthenticationRequired
    }.getOrDefault(false)

    private fun createAndWrap(wrappedFile: File, key: SecretKey): ByteArray {
        val plain = ByteArray(PASSPHRASE_BYTES).also(SecureRandom()::nextBytes)
        wrap(plain, key, wrappedFile)
        return plain
    }

    private fun wrap(plain: ByteArray, key: SecretKey, wrappedFile: File) {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(plain)
        wrappedFile.outputStream().use { output ->
            output.write(cipher.iv.size)
            output.write(cipher.iv)
            output.write(encrypted)
        }
    }

    private fun unwrap(wrappedFile: File, key: SecretKey): ByteArray {
        val bytes = wrappedFile.readBytes()
        val ivSize = bytes[0].toInt()
        val iv = bytes.copyOfRange(1, 1 + ivSize)
        val encrypted = bytes.copyOfRange(1 + ivSize, bytes.size)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        } catch (invalidated: KeyPermanentlyInvalidatedException) {
            throw PassphraseInvalidatedException(invalidated)
        }
        return cipher.doFinal(encrypted)
    }

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "offhand_db_passphrase_key"
        const val WRAPPED_PASSPHRASE_FILE = "db_passphrase.bin"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val PASSPHRASE_BYTES = 32
        const val GCM_TAG_BITS = 128
        const val LOG_TAG = "Security"
    }
}
