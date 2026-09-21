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
import com.dmytrosamoilov.offhand.feature.notes.domain.export.DocxWriter
import com.dmytrosamoilov.offhand.feature.notes.domain.export.NoteDocumentBuilder
import com.dmytrosamoilov.offhand.feature.notes.domain.export.NoteExportFormat
import com.dmytrosamoilov.offhand.feature.notes.domain.export.NotePdfRenderer
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AndroidPrepareNoteShareUseCase(
    private val audioStore: EncryptedAudioStore,
    private val noteShareLabelsProvider: NoteShareLabelsProvider,
    private val shareCacheDirectoryProvider: ShareCacheDirectoryProvider,
    private val dateLabelFormatter: DateLabelFormatter,
    private val documentBuilder: NoteDocumentBuilder,
    private val pdfRenderer: NotePdfRenderer,
) : PrepareNoteShareUseCase {

    override suspend fun invoke(
        note: Note,
        noteFormat: NoteExportFormat?,
        includeAudio: Boolean,
    ): NoteShareBundle = withContext(Dispatchers.IO) {
        val shareDir = prepareShareDir()
        val baseName = fileBaseName(note)
        val files = buildList {
            if (noteFormat != null) add(writeNoteFile(shareDir, baseName, note, noteFormat))
            val audioFileName = note.audioFileName
            if (includeAudio && audioFileName != null) {
                add(writeAudioFile(shareDir, baseName, audioFileName))
            }
        }
        check(files.isNotEmpty())
        NoteShareBundle(
            filePaths = files.map { it.absolutePath },
            mimeType = mimeType(files.size, noteFormat),
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

    private suspend fun writeNoteFile(shareDir: File, baseName: String, note: Note, format: NoteExportFormat): File {
        val file = File(shareDir, "$baseName.${format.fileExtension}")
        when (format) {
            NoteExportFormat.TEXT -> file.writeText(textContent(note))
            NoteExportFormat.DOCX -> file.writeBytes(DocxWriter.write(documentBuilder.build(note)))
            NoteExportFormat.PDF -> pdfRenderer.render(documentBuilder.build(note), file.absolutePath)
        }
        return file
    }

    private fun textContent(note: Note): String = NoteShareFormatter.textContent(
        labels = noteShareLabelsProvider.labels(),
        title = note.title,
        formattedDate = dateLabelFormatter.dateTime(note.createdAtLocalDateTime()),
        overview = note.body,
        transcript = note.transcript,
    )

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

    private fun mimeType(fileCount: Int, noteFormat: NoteExportFormat?): String = when {
        fileCount > 1 -> MIME_ANY
        noteFormat != null -> noteFormat.mimeType
        else -> MIME_AUDIO
    }

    private companion object {
        const val MIME_ANY = "*/*"
        const val MIME_AUDIO = "audio/wav"
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
    }
}
