@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.audio.StreamingAudioRecorder
import com.dmytrosamoilov.offhand.core.audio.WavCodec
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareBundle
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabelsProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.ShareCacheDirectoryProvider
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class PrepareNoteShareUseCase @Inject constructor(
    private val audioStore: EncryptedAudioStore,
    private val noteShareLabelsProvider: NoteShareLabelsProvider,
    private val shareCacheDirectoryProvider: ShareCacheDirectoryProvider,
    private val dateLabelFormatter: DateLabelFormatter,
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
            filePaths = files.map { it.absolutePath },
            mimeType = mimeType(files.size, includeNote),
        )
    }

    private fun prepareShareDir(): File {
        val shareDir = File(shareCacheDirectoryProvider.shareDirectoryPath())
        shareDir.deleteRecursively()
        shareDir.mkdirs()
        return shareDir
    }

    private fun fileBaseName(note: Note): String = NoteShareFormatter.fileBaseName(
        title = note.title,
        fallbackTitle = noteShareLabelsProvider.fallbackTitle(),
        createdAtEpochMs = note.createdAtEpochMs,
        zone = TimeZone.currentSystemDefault(),
    )

    private fun writeNoteFile(shareDir: File, baseName: String, note: Note): File {
        val file = File(shareDir, "$baseName.txt")
        file.writeText(
            NoteShareFormatter.textContent(
                labels = noteShareLabelsProvider.labels(),
                title = note.title,
                formattedDate = dateLabelFormatter.dateTime(note.createdAtLocalDateTime()),
                overview = note.body,
                transcript = note.transcript,
            ),
        )
        return file
    }

    private fun Note.createdAtLocalDateTime(): LocalDateTime =
        Instant.fromEpochMilliseconds(createdAtEpochMs).toLocalDateTime(TimeZone.currentSystemDefault())

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

    private fun mimeType(fileCount: Int, includeNote: Boolean): String = when {
        fileCount > 1 -> MIME_ANY
        includeNote -> MIME_TEXT
        else -> MIME_AUDIO
    }

    private companion object {
        const val MIME_ANY = "*/*"
        const val MIME_TEXT = "text/plain"
        const val MIME_AUDIO = "audio/wav"
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
    }
}
