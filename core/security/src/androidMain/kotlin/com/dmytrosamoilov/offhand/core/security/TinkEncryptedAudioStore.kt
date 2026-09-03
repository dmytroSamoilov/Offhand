package com.dmytrosamoilov.offhand.core.security

import android.content.Context
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.UUID

class TinkEncryptedAudioStore(
    private val context: Context,
) : EncryptedAudioStore {

    private val streamingAead: StreamingAead by lazy {
        StreamingAeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get(KEY_TEMPLATE))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(StreamingAead::class.java)
    }

    private val recordingsDir: File
        get() = File(context.filesDir, RECORDINGS_DIR).apply { mkdirs() }

    override fun newRecordingFileName(): String = "note-${UUID.randomUUID()}$FILE_EXTENSION"

    override fun openForWrite(fileName: String): AudioOutputStream {
        val file = fileFor(fileName)
        return AudioOutputStream(
            streamingAead.newEncryptingStream(
                FileOutputStream(file),
                fileName.toByteArray(),
            ),
        )
    }

    override fun openForRead(fileName: String): AudioInputStream =
        AudioInputStream(
            streamingAead.newDecryptingStream(
                FileInputStream(fileFor(fileName)),
                fileName.toByteArray(),
            ),
        )

    override fun openSeekableForRead(fileName: String): AudioSeekableChannel =
        AudioSeekableChannel(
            streamingAead.newSeekableDecryptingChannel(
                FileChannel.open(fileFor(fileName).toPath(), StandardOpenOption.READ),
                fileName.toByteArray(),
            ),
        )

    override fun delete(fileName: String) {
        fileFor(fileName).delete()
    }

    override fun sizeOf(fileName: String): Long = fileFor(fileName).length()

    override fun pcmSizeOf(fileName: String): Long =
        openSeekableForRead(fileName).use { it.size() }

    override fun deleteUnreferenced(referencedFileNames: Set<String>, minAgeMs: Long): Int {
        val cutoffMs = System.currentTimeMillis() - minAgeMs
        return recordingsDir.listFiles().orEmpty()
            .filter { it.name !in referencedFileNames && it.lastModified() < cutoffMs }
            .count { it.delete() }
    }

    private fun fileFor(fileName: String): File {
        val file = File(recordingsDir, fileName)
        require(file.canonicalPath.startsWith(recordingsDir.canonicalPath + File.separator)) {
            "Recording file name escapes the recordings directory"
        }
        return file
    }

    private companion object {
        const val KEYSET_NAME = "audio_keyset"
        const val KEYSET_PREFS = "audio_keyset_prefs"
        const val MASTER_KEY_URI = "android-keystore://offhand_audio_master_key"
        const val KEY_TEMPLATE = "AES256_GCM_HKDF_4KB"
        const val RECORDINGS_DIR = "recordings"
        const val FILE_EXTENSION = ".pcm.enc"
    }
}
