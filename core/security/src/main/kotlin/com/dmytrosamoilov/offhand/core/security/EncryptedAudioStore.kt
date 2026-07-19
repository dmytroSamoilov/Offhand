package com.dmytrosamoilov.offhand.core.security

import android.content.Context
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.nio.channels.SeekableByteChannel
import java.nio.file.StandardOpenOption
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedAudioStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

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

    fun newRecordingFileName(): String = "note-${UUID.randomUUID()}$FILE_EXTENSION"

    fun openForWrite(fileName: String): OutputStream {
        val file = fileFor(fileName)
        return streamingAead.newEncryptingStream(
            FileOutputStream(file),
            fileName.toByteArray(),
        )
    }

    fun openForRead(fileName: String): InputStream =
        streamingAead.newDecryptingStream(
            FileInputStream(fileFor(fileName)),
            fileName.toByteArray(),
        )

    fun openSeekableForRead(fileName: String): SeekableByteChannel =
        streamingAead.newSeekableDecryptingChannel(
            FileChannel.open(fileFor(fileName).toPath(), StandardOpenOption.READ),
            fileName.toByteArray(),
        )

    fun delete(fileName: String) {
        fileFor(fileName).delete()
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
