package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.dmytrosamoilov.offhand.core.audio.StreamingAudioRecorder
import com.dmytrosamoilov.offhand.core.audio.WavCodec
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.feature.notes.R
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareBundle
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabels
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrepareNoteShareUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioStore: EncryptedAudioStore,
) {

    suspend operator fun invoke(
        note: Note,
        includeNote: Boolean,
        includeAudio: Boolean,
    ): NoteShareBundle = withContext(Dispatchers.IO) {
        val shareDir = prepareShareDir()
        val baseName = fileBaseName(note)
        val files = buildList {
            if (includeNote) add(writeNoteFile(shareDir, baseName, note))
            val audioFileName = note.audioFileName
            if (includeAudio && audioFileName != null) {
                add(writeAudioFile(shareDir, baseName, audioFileName))
            }
        }
        check(files.isNotEmpty())
        NoteShareBundle(
            uris = files.map(::toContentUri),
            mimeType = mimeType(files.size, includeNote),
        )
    }

    private fun prepareShareDir(): File {
        val shareDir = File(context.cacheDir, SHARE_DIR)
        shareDir.deleteRecursively()
        shareDir.mkdirs()
        return shareDir
    }

    private fun fileBaseName(note: Note): String = NoteShareFormatter.fileBaseName(
        title = note.title,
        fallbackTitle = context.getString(R.string.notes_recording_fallback_title),
        createdAtEpochMs = note.createdAtEpochMs,
        zone = ZoneId.systemDefault(),
    )

    private fun writeNoteFile(shareDir: File, baseName: String, note: Note): File {
        val file = File(shareDir, "$baseName.txt")
        file.writeText(
            NoteShareFormatter.textContent(
                labels = shareLabels(),
                title = note.title,
                createdAtEpochMs = note.createdAtEpochMs,
                overview = note.body,
                transcript = note.transcript,
                zone = ZoneId.systemDefault(),
            ),
        )
        return file
    }

    private fun shareLabels(): NoteShareLabels = NoteShareLabels(
        title = context.getString(R.string.notes_edit_title_label),
        date = context.getString(R.string.notes_share_date_label),
        overview = context.getString(R.string.notes_overview_heading),
        transcript = context.getString(R.string.notes_transcript_heading),
    )

    private fun writeAudioFile(shareDir: File, baseName: String, audioFileName: String): File {
        val file = File(shareDir, "$baseName.wav")
        FileOutputStream(file).use { output ->
            output.write(ByteArray(WavCodec.HEADER_BYTES))
            audioStore.openForRead(audioFileName).use { it.copyTo(output) }
        }
        writeWavHeader(file)
        return file
    }

    private fun writeWavHeader(file: File) {
        val dataSize = (file.length() - WavCodec.HEADER_BYTES).toInt()
        RandomAccessFile(file, "rw").use {
            it.write(
                WavCodec.header(
                    dataSize = dataSize,
                    sampleRate = StreamingAudioRecorder.SAMPLE_RATE,
                    channels = CHANNELS,
                    bitsPerSample = BITS_PER_SAMPLE,
                ),
            )
        }
    }

    private fun toContentUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)

    private fun mimeType(fileCount: Int, includeNote: Boolean): String = when {
        fileCount > 1 -> MIME_ANY
        includeNote -> MIME_TEXT
        else -> MIME_AUDIO
    }

    private companion object {
        const val SHARE_DIR = "shared_notes"
        const val AUTHORITY_SUFFIX = ".fileprovider"
        const val MIME_ANY = "*/*"
        const val MIME_TEXT = "text/plain"
        const val MIME_AUDIO = "audio/wav"
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
    }
}
