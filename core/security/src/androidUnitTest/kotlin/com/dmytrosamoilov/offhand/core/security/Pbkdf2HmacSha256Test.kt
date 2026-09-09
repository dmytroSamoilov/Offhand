package com.dmytrosamoilov.offhand.core.security

import kotlin.test.Test
import kotlin.test.assertContentEquals

class Pbkdf2HmacSha256Test {

    @Test
    fun `matches the published test vector for one iteration`() {
        val key = Pbkdf2HmacSha256.derive("password".encodeToByteArray(), "salt".encodeToByteArray(), 1, 32)

        assertContentEquals("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b".hexToBytes(), key)
    }

    @Test
    fun `matches the published test vector for many iterations and two blocks`() {
        val key = Pbkdf2HmacSha256.derive("password".encodeToByteArray(), "salt".encodeToByteArray(), 4096, 40)

        assertContentEquals("c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134af7ad98c1b458ce3f".hexToBytes(), key)
    }

    @Test
    fun `uses the raw passphrase bytes so non-ASCII passphrases match other platforms`() {
        val passphrase = "pässwörd".encodeToByteArray()

        val fromBytes = Pbkdf2HmacSha256.derive(passphrase, "salt".encodeToByteArray(), 2, 32)
        val expected = javax.crypto.Mac.getInstance("HmacSHA256").let { mac ->
            mac.init(javax.crypto.spec.SecretKeySpec(passphrase, "HmacSHA256"))
            mac.update("salt".encodeToByteArray())
            val u1 = mac.doFinal(byteArrayOf(0, 0, 0, 1))
            val u2 = mac.doFinal(u1)
            ByteArray(32) { (u1[it].toInt() xor u2[it].toInt()).toByte() }
        }

        assertContentEquals(expected, fromBytes)
    }

    private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
