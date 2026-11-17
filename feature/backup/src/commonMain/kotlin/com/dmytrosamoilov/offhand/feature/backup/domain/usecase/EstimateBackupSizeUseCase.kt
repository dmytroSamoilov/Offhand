package com.dmytrosamoilov.offhand.feature.backup.domain.usecase

import com.dmytrosamoilov.offhand.core.data.domain.CustomNoteStylesRepository
import com.dmytrosamoilov.offhand.core.data.domain.FoldersRepository
import com.dmytrosamoilov.offhand.core.data.domain.Note
import com.dmytrosamoilov.offhand.core.data.domain.NoteStatus
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.security.EncryptedAudioStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class BackupSizeEstimate(
    val textBytes: Long,
    val recordingBytes: Long,
)

class EstimateBackupSizeUseCase(
    private val notesRepository: NotesRepository,
    private val foldersRepository: FoldersRepository,
    private val customNoteStylesRepository: CustomNoteStylesRepository,
    private val audioStore: EncryptedAudioStore,
) {

    suspend operator fun invoke(): BackupSizeEstimate = withContext(Dispatchers.IO) {
        val notes = notesRepository.observeNotes().first().filter { it.status == NoteStatus.READY || it.status == NoteStatus.FAILED }
        val folderBytes = foldersRepository.observeFolders().first().sumOf { it.name.utf8Size() + RECORD_OVERHEAD_BYTES }
        val styleBytes = customNoteStylesRepository.observeStyles().first().size.toLong() * STYLE_BYTES
        BackupSizeEstimate(
            textBytes = HEADER_BYTES + folderBytes + styleBytes + notes.sumOf { it.textBytes() },
            recordingBytes = notes.mapNotNull { it.audioFileName }.distinct().sumOf { fileName ->
                runCatching { audioStore.pcmSizeOf(fileName) }.getOrDefault(0L) + RECORD_OVERHEAD_BYTES
            },
        )
    }

    private fun Note.textBytes(): Long =
        title.utf8Size() + body.utf8Size() + transcript.utf8Size() + RECORD_OVERHEAD_BYTES

    private fun String.utf8Size(): Long = encodeToByteArray().size.toLong()

    private companion object {
        const val HEADER_BYTES = 4_096L
        const val RECORD_OVERHEAD_BYTES = 256L
        const val STYLE_BYTES = 2_048L
    }
}
