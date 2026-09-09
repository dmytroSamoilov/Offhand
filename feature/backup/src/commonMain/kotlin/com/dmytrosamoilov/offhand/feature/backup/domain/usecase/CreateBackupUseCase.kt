@file:OptIn(ExperimentalTime::class)

package com.dmytrosamoilov.offhand.feature.backup.domain.usecase

import com.dmytrosamoilov.offhand.core.common.BuildInfo
import com.dmytrosamoilov.offhand.core.data.domain.Folder
import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.security.BackupCrypto
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.core.security.closeQuietly
import com.dmytrosamoilov.offhand.core.security.readChunk
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupFile
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupFolder
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupManifest
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupNote
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupSummary
import com.dmytrosamoilov.offhand.feature.backup.domain.archive.BackupArchive
import com.dmytrosamoilov.offhand.feature.backup.domain.archive.BackupFormat
import com.dmytrosamoilov.offhand.feature.backup.domain.archive.RecordKind
import com.dmytrosamoilov.offhand.feature.backup.domain.archive.writeEndRecord
import com.dmytrosamoilov.offhand.feature.backup.domain.archive.writeRecordHeader
import com.dmytrosamoilov.offhand.feature.backup.domain.archive.writeTextRecord
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.use

class CreateBackupUseCase(
    private val notesRepository: NotesRepository,
    private val foldersRepository: FoldersRepository,
    private val audioStore: EncryptedAudioStore,
    private val crypto: BackupCrypto,
    private val buildInfo: BuildInfo,
) {

    suspend operator fun invoke(
        file: BackupFile,
        passphrase: ByteArray,
        includeAudio: Boolean,
        onProgress: (Int) -> Unit = {},
    ): BackupSummary = withContext(Dispatchers.IO) {
        val notes = notesRepository.observeNotes().first().filter { it.status.isSettled() }
        val folders = foldersRepository.observeFolders().first()
        val audioSizes = if (includeAudio) audioSizesOf(notes) else emptyMap()
        val totalAudioBytes = audioSizes.values.sum()
        val sink = BackupArchive(crypto).openForWrite(file.openWrite(), passphrase)
        sink.use { out ->
            out.writeTextRecord(RecordKind.MANIFEST, json.encodeToString(manifest(notes, folders, includeAudio, totalAudioBytes)))
            out.writeTextRecord(RecordKind.FOLDERS, json.encodeToString(folders.map(Folder::toBackup)))
            out.writeTextRecord(RecordKind.NOTES, json.encodeToString(notes.map { it.toBackup(includeAudio) }))
            val progress = ProgressTracker(totalAudioBytes, onProgress)
            audioSizes.forEach { (fileName, size) -> out.writeAudioRecord(fileName, size, progress) }
            out.writeEndRecord()
        }
        onProgress(COMPLETE_PERCENT)
        BackupSummary(notes = notes.size, folders = folders.size, audioBytes = totalAudioBytes)
    }

    private fun audioSizesOf(notes: List<Note>): Map<String, Long> =
        notes.mapNotNull { it.audioFileName }.distinct().associateWith { fileName ->
            runCatching { audioStore.pcmSizeOf(fileName) }.getOrDefault(0L)
        }.filterValues { it > 0L }

    private fun BufferedSink.writeAudioRecord(fileName: String, size: Long, progress: ProgressTracker) {
        writeRecordHeader(RecordKind.AUDIO, fileName, size)
        val stream = audioStore.openForRead(fileName)
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        var remaining = size
        try {
            while (remaining > 0) {
                val read = stream.readChunk(buffer)
                if (read <= 0) break
                val count = minOf(read.toLong(), remaining).toInt()
                write(buffer, 0, count)
                remaining -= count
                progress.advance(count.toLong())
            }
        } finally {
            stream.closeQuietly()
        }
        if (remaining > 0) write(ByteArray(remaining.toInt()))
    }

    private fun manifest(notes: List<Note>, folders: List<Folder>, includeAudio: Boolean, totalAudioBytes: Long) =
        BackupManifest(
            formatVersion = BackupFormat.VERSION,
            appVersion = buildInfo.appVersion,
            platform = buildInfo.platform,
            createdAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            noteCount = notes.size,
            folderCount = folders.size,
            includesAudio = includeAudio,
            totalAudioBytes = totalAudioBytes,
        )

    private class ProgressTracker(private val total: Long, private val onProgress: (Int) -> Unit) {
        private var done = 0L
        private var lastPercent = -1

        fun advance(bytes: Long) {
            done += bytes
            val percent = if (total <= 0L) 0 else ((done * COMPLETE_PERCENT) / total).toInt().coerceIn(0, COMPLETE_PERCENT - 1)
            if (percent != lastPercent) {
                lastPercent = percent
                onProgress(percent)
            }
        }
    }

    private companion object {
        const val COPY_BUFFER_BYTES = 64 * 1024
        const val COMPLETE_PERCENT = 100
        val json = Json { encodeDefaults = true }
    }
}

internal fun NoteStatus.isSettled(): Boolean = this == NoteStatus.READY || this == NoteStatus.FAILED

internal fun Folder.toBackup(): BackupFolder = BackupFolder(id = id, name = name, createdAtEpochMs = createdAtEpochMs)

internal fun Note.toBackup(includeAudio: Boolean): BackupNote = BackupNote(
    id = id,
    title = title,
    body = body,
    transcript = transcript,
    createdAtEpochMs = createdAtEpochMs,
    transcriptionTimeMs = transcriptionTimeMs,
    structuringTimeMs = structuringTimeMs,
    hardwareBackend = hardwareBackend,
    audioFileName = if (includeAudio) audioFileName else null,
    durationMs = durationMs,
    status = status.name,
    preset = preset.name,
    folderId = folderId,
)
