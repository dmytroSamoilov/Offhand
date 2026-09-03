@file:OptIn(ExperimentalForeignApi::class, ExperimentalTime::class, BetaInteropApi::class)

package com.dmytrosamoilov.offhand.feature.notes.domain.usecase

import com.dmytrosamoilov.offhand.core.audio.WavCodec
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.feature.notes.domain.DateLabelFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareBundle
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareFormatter
import com.dmytrosamoilov.offhand.feature.notes.domain.NoteShareLabelsProvider
import com.dmytrosamoilov.offhand.feature.notes.domain.ShareCacheDirectoryProvider
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSData
import platform.Foundation.NSDataWritingAtomic
import platform.Foundation.NSDataWritingFileProtectionCompleteUnlessOpen
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileProtectionCompleteUnlessOpen
import platform.Foundation.NSFileProtectionKey
import platform.Foundation.closeFile
import platform.Foundation.create
import platform.Foundation.fileHandleForWritingAtPath
import platform.Foundation.seekToFileOffset
import platform.Foundation.writeData
import platform.Foundation.writeToFile

class IosPrepareNoteShareUseCase(
    private val audioStore: EncryptedAudioStore,
    private val noteShareLabelsProvider: NoteShareLabelsProvider,
    private val shareCacheDirectoryProvider: ShareCacheDirectoryProvider,
    private val dateLabelFormatter: DateLabelFormatter,
) : PrepareNoteShareUseCase {

    override suspend fun invoke(
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
            filePaths = files,
            mimeType = mimeType(files.size, includeNote),
        )
    }

    private fun prepareShareDir(): String {
        val shareDir = shareCacheDirectoryProvider.shareDirectoryPath()
        NSFileManager.defaultManager.removeItemAtPath(shareDir, error = null)
        NSFileManager.defaultManager.createDirectoryAtPath(
            shareDir,
            withIntermediateDirectories = true,
            attributes = mapOf(NSFileProtectionKey to NSFileProtectionCompleteUnlessOpen),
            error = null,
        )
        return shareDir
    }

    private fun fileBaseName(note: Note): String = NoteShareFormatter.fileBaseName(
        title = note.title,
        fallbackTitle = noteShareLabelsProvider.fallbackTitle(),
        createdAtEpochMs = note.createdAtEpochMs,
        zone = TimeZone.currentSystemDefault(),
    )

    private fun writeNoteFile(shareDir: String, baseName: String, note: Note): String {
        val path = "$shareDir/$baseName.txt"
        val content = NoteShareFormatter.textContent(
            labels = noteShareLabelsProvider.labels(),
            title = note.title,
            formattedDate = dateLabelFormatter.dateTime(note.createdAtLocalDateTime()),
            overview = note.body,
            transcript = note.transcript,
        )
        content.encodeToByteArray().toNsData().writeToFile(
            path,
            options = NSDataWritingAtomic or NSDataWritingFileProtectionCompleteUnlessOpen,
            error = null,
        )
        return path
    }

    private fun Note.createdAtLocalDateTime(): LocalDateTime =
        Instant.fromEpochMilliseconds(createdAtEpochMs).toLocalDateTime(TimeZone.currentSystemDefault())

    private fun writeAudioFile(shareDir: String, baseName: String, audioFileName: String): String {
        val path = "$shareDir/$baseName.wav"
        NSFileManager.defaultManager.createFileAtPath(
            path,
            contents = null,
            attributes = mapOf(NSFileProtectionKey to NSFileProtectionCompleteUnlessOpen),
        )
        val handle = requireNotNull(NSFileHandle.fileHandleForWritingAtPath(path))
        var pcmBytes = 0
        try {
            handle.writeData(ByteArray(WavCodec.HEADER_BYTES).toNsData())
            val input = audioStore.openForRead(audioFileName)
            try {
                while (true) {
                    val chunk = input.read(COPY_CHUNK_BYTES) ?: break
                    handle.writeData(chunk.toNsData())
                    pcmBytes += chunk.size
                }
            } finally {
                input.close()
            }
            handle.seekToFileOffset(0uL)
            handle.writeData(
                WavCodec.header(
                    dataSize = pcmBytes,
                    sampleRate = SAMPLE_RATE,
                    channels = CHANNELS,
                    bitsPerSample = BITS_PER_SAMPLE,
                ).toNsData(),
            )
        } finally {
            handle.closeFile()
        }
        return path
    }

    private fun mimeType(fileCount: Int, includeNote: Boolean): String = when {
        fileCount > 1 -> MIME_ANY
        includeNote -> MIME_TEXT
        else -> MIME_AUDIO
    }

    private fun ByteArray.toNsData(): NSData = if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
    }

    private companion object {
        const val MIME_ANY = "*/*"
        const val MIME_TEXT = "text/plain"
        const val MIME_AUDIO = "audio/wav"
        const val SAMPLE_RATE = 16_000
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
        const val COPY_CHUNK_BYTES = 512L * 1024L
    }
}
