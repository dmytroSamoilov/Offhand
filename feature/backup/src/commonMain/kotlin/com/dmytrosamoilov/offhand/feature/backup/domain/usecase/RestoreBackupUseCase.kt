package com.dmytrosamoilov.offhand.feature.backup.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NotePreset
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.security.BackupCrypto
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import com.dmytrosamoilov.offhand.core.security.closeQuietly
import com.dmytrosamoilov.offhand.core.security.writeChunk
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupException
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupFile
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupFolder
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupManifest
import com.dmytrosamoilov.offhand.feature.backup.domain.BackupNote
import com.dmytrosamoilov.offhand.feature.backup.domain.RestoreSummary
import com.dmytrosamoilov.offhand.feature.backup.domain.archive.BackupArchive
import com.dmytrosamoilov.offhand.feature.backup.domain.archive.RecordHeader
import com.dmytrosamoilov.offhand.feature.backup.domain.archive.RecordKind
import com.dmytrosamoilov.offhand.feature.backup.domain.archive.readRecordHeader
import com.dmytrosamoilov.offhand.feature.backup.domain.archive.readTextRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.BufferedSource
import okio.use

class RestoreBackupUseCase(
    private val notesRepository: NotesRepository,
    private val foldersRepository: FoldersRepository,
    private val audioStore: EncryptedAudioStore,
    private val crypto: BackupCrypto,
) {

    suspend operator fun invoke(
        file: BackupFile,
        passphrase: ByteArray,
        onProgress: (Int) -> Unit = {},
    ): RestoreSummary = withContext(Dispatchers.IO) {
        val source = BackupArchive(crypto).openForRead(file.openRead(), passphrase)
        source.use { input ->
            val manifest = input.readManifest()
            val session = RestoreSession(manifest, onProgress)
            while (true) {
                val header = input.readRecordHeader()
                when (header.kind) {
                    RecordKind.END -> break
                    RecordKind.MANIFEST -> throw BackupException.Corrupt("Duplicate manifest")
                    RecordKind.FOLDERS -> session.restoreFolders(json.decodeFromString(input.readTextRecord(header)))
                    RecordKind.NOTES -> session.restoreNotes(json.decodeFromString(input.readTextRecord(header)))
                    RecordKind.AUDIO -> session.restoreAudio(header, input)
                }
            }
            onProgress(COMPLETE_PERCENT)
            session.summary()
        }
    }

    private fun BufferedSource.readManifest(): BackupManifest {
        val header = readRecordHeader()
        if (header.kind != RecordKind.MANIFEST) throw BackupException.Corrupt("Backup does not start with a manifest")
        return json.decodeFromString(readTextRecord(header))
    }

    private inner class RestoreSession(
        private val manifest: BackupManifest,
        private val onProgress: (Int) -> Unit,
    ) {
        private val folderIds = mutableMapOf<Long, Long>()
        private val noteIdsByAudioName = mutableMapOf<String, Long>()
        private var notesRestored = 0
        private var notesSkipped = 0
        private var foldersCreated = 0
        private var foldersReused = 0
        private var audioBytesDone = 0L

        suspend fun restoreFolders(folders: List<BackupFolder>) {
            val existing = foldersRepository.observeFolders().first()
            folders.forEach { folder ->
                val match = existing.firstOrNull { it.name.equals(folder.name, ignoreCase = true) }
                folderIds[folder.id] = match?.id?.also { foldersReused += 1 }
                    ?: foldersRepository.createFolder(folder.name).also { foldersCreated += 1 }
            }
        }

        suspend fun restoreNotes(notes: List<BackupNote>) {
            val existing = notesRepository.observeNotes().first()
            notes.forEach { note ->
                if (existing.any { it.isSameAs(note) }) {
                    notesSkipped += 1
                } else {
                    val newId = notesRepository.createNote(note.toDomain(folderIds))
                    note.audioFileName?.let { noteIdsByAudioName[it] = newId }
                    notesRestored += 1
                }
            }
            reportProgress()
        }

        suspend fun restoreAudio(header: RecordHeader, input: BufferedSource) {
            val noteId = noteIdsByAudioName.remove(header.name)
            if (noteId == null) {
                input.skip(header.length)
                audioBytesDone += header.length
                reportProgress()
                return
            }
            val fileName = audioStore.newRecordingFileName()
            copyAudio(input, header.length, fileName)
            val note = notesRepository.getNote(noteId) ?: return
            notesRepository.updateNote(note.copy(audioFileName = fileName))
        }

        private fun copyAudio(input: BufferedSource, length: Long, fileName: String) {
            val stream = audioStore.openForWrite(fileName)
            val buffer = ByteArray(COPY_BUFFER_BYTES)
            var remaining = length
            try {
                while (remaining > 0) {
                    val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (count <= 0) throw BackupException.Corrupt("Audio record is truncated")
                    stream.writeChunk(buffer, count)
                    remaining -= count
                    audioBytesDone += count
                    reportProgress()
                }
            } finally {
                stream.closeQuietly()
            }
        }

        private fun reportProgress() {
            val total = manifest.totalAudioBytes
            val percent = if (total <= 0L) {
                if (manifest.noteCount == 0) 0 else ((notesRestored + notesSkipped) * COMPLETE_PERCENT / manifest.noteCount)
            } else {
                (audioBytesDone * COMPLETE_PERCENT / total).toInt()
            }
            onProgress(percent.coerceIn(0, COMPLETE_PERCENT - 1))
        }

        fun summary() = RestoreSummary(
            notesRestored = notesRestored,
            notesSkipped = notesSkipped,
            foldersCreated = foldersCreated,
            foldersReused = foldersReused,
        )
    }

    private companion object {
        const val COPY_BUFFER_BYTES = 64 * 1024
        const val COMPLETE_PERCENT = 100
        val json = Json { ignoreUnknownKeys = true }
    }
}

internal fun Note.isSameAs(backup: BackupNote): Boolean =
    createdAtEpochMs == backup.createdAtEpochMs && title == backup.title

internal fun BackupNote.toDomain(folderIds: Map<Long, Long>): Note = Note(
    id = 0,
    title = title,
    body = body,
    transcript = transcript,
    createdAtEpochMs = createdAtEpochMs,
    transcriptionTimeMs = transcriptionTimeMs,
    structuringTimeMs = structuringTimeMs,
    hardwareBackend = hardwareBackend,
    audioFileName = null,
    durationMs = durationMs,
    status = NoteStatus.entries.firstOrNull { it.name == status }?.takeIf { it.isSettled() } ?: NoteStatus.READY,
    preset = NotePreset.fromName(preset),
    folderId = folderId?.let(folderIds::get),
)
