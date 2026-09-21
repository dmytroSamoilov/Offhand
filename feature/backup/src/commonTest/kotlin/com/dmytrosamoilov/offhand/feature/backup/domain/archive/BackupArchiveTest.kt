package com.dmytrosamoilov.offhand.feature.backup.domain.archive

import com.dmytrosamoilov.offhand.feature.backup.domain.BackupException
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import okio.Buffer
import okio.use

class BackupArchiveTest {

    private val crypto = FakeBackupCrypto()
    private val passphrase = "correct horse".encodeToByteArray()

    @Test
    fun `records round trip through the encrypted container`() {
        val audio = Random(7).nextBytes(3 * BackupFormat.CHUNK_BYTES + 123)
        val file = Buffer()

        BackupArchive(crypto).openForWrite(file, passphrase).use { out ->
            out.writeTextRecord(RecordKind.MANIFEST, "{\"v\":1}")
            out.writeRecordHeader(RecordKind.AUDIO, "note-1.pcm.enc", audio.size.toLong())
            out.write(audio)
            out.writeEndRecord()
        }

        BackupArchive(crypto).openForRead(file, passphrase).use { input ->
            val manifest = input.readRecordHeader()
            assertEquals(RecordKind.MANIFEST, manifest.kind)
            assertEquals("{\"v\":1}", input.readTextRecord(manifest))
            val audioHeader = input.readRecordHeader()
            assertEquals(RecordHeader(RecordKind.AUDIO, "note-1.pcm.enc", audio.size.toLong()), audioHeader)
            assertContentEquals(audio, input.readByteArray(audioHeader.length))
            assertEquals(RecordKind.END, input.readRecordHeader().kind)
        }
    }

    @Test
    fun `wrong passphrase is reported as such`() {
        val file = writeSmallArchive()

        assertFailsWith<BackupException.WrongPassphrase> {
            BackupArchive(crypto).openForRead(file, "other".encodeToByteArray()).readRecordHeader()
        }
    }

    @Test
    fun `truncated archive is reported as corrupt`() {
        val file = writeSmallArchive()
        val truncated = Buffer().write(file.readByteArray(file.size - 5))

        assertFailsWith<BackupException.Corrupt> {
            BackupArchive(crypto).openForRead(truncated, passphrase).use { input ->
                input.readRecordHeader()
                input.readByteArray()
            }
        }
    }

    @Test
    fun `foreign file is rejected`() {
        val file = Buffer().write("PK not a backup at all".encodeToByteArray())

        assertFailsWith<BackupException.Corrupt> { BackupArchive(crypto).openForRead(file, passphrase) }
    }

    @Test
    fun `newer format version is reported`() {
        val file = writeSmallArchive()
        val bytes = file.readByteArray()
        bytes[BackupFormat.MAGIC.size] = 9

        assertFailsWith<BackupException.UnsupportedVersion> {
            BackupArchive(crypto).openForRead(Buffer().write(bytes), passphrase)
        }
    }

    private fun writeSmallArchive(): Buffer = Buffer().also { file ->
        BackupArchive(crypto).openForWrite(file, passphrase).use { out ->
            out.writeTextRecord(RecordKind.MANIFEST, "{}")
            out.writeEndRecord()
        }
    }
}
