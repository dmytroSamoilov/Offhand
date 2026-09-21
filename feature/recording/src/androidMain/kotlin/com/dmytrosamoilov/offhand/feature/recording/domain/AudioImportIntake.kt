package com.dmytrosamoilov.offhand.feature.recording.domain

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.dmytrosamoilov.offhand.core.data.domain.AudioImportSource
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Content URIs from the share sheet and the document picker are only granted to
// the receiving activity, so the file is copied into the app cache first and the
// decoder deletes that copy once the encrypted recording exists.
class AudioImportIntake(
    private val context: Context,
) {

    suspend fun stage(uri: Uri): AudioImportSource? = withContext(Dispatchers.IO) {
        val displayName = displayNameOf(uri)
        val target = File(importDirectory(), UUID.randomUUID().toString())
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }.getOrNull()
        if (copied == null) {
            target.delete()
            return@withContext null
        }
        AudioImportSource(handle = target.absolutePath, displayName = displayName)
    }

    // Staged copies that never reached the decoder, such as a share that
    // ended at a declined paywall.
    suspend fun discard(sources: List<AudioImportSource>) = withContext(Dispatchers.IO) {
        sources.forEach { source -> File(source.handle).delete() }
    }

    private fun importDirectory(): File = File(context.cacheDir, IMPORT_DIRECTORY).apply { mkdirs() }

    private fun displayNameOf(uri: Uri): String {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: DEFAULT_NAME
    }

    private companion object {
        const val IMPORT_DIRECTORY = "imports"
        const val DEFAULT_NAME = "audio"
    }
}
